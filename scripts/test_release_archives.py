"""Publication must stop before deleting original assets if validation fails."""
import copy
import json
import pathlib
import tempfile
import unittest
from unittest.mock import patch

import publish_release_archives as publisher


class PublicationSafetyTests(unittest.TestCase):
    def setUp(self):
        self.plan = json.loads(publisher.PLAN_FILE.read_text())
        self.jar = next(a for a in self.plan['assets'] if a['name'].endswith('.jar'))
        self.release = {
            'assets': [copy.deepcopy(self.jar)],
            'body': self.plan['original_body'],
            'html_url': 'https://github.com/hotamachisubaru-git/Miniutility/releases/tag/26.3.1',
            'author': {'login': 'hotamachisubaru-git'},
        }

    def test_mismatched_remote_zip_prevents_notes_and_deletion(self):
        with patch.object(publisher, 'release_snapshot', return_value=self.release), \
             patch.object(publisher, 'verify_uploaded', side_effect=RuntimeError('checksum mismatch')), \
             patch.object(publisher, 'gh') as gh:
            with self.assertRaisesRegex(RuntimeError, 'checksum mismatch'):
                publisher.publish(self.plan, pathlib.Path('/unused'))
            self.assertTrue(gh.called)
            self.assertTrue(all(c.args[:2] == ('release', 'upload') for c in gh.call_args_list))

    def test_replaced_legacy_asset_is_not_deleted(self):
        self.release['assets'][0]['id'] += 1
        with patch.object(publisher, 'release_snapshot', return_value=self.release), \
             patch.object(publisher, 'gh') as gh:
            with self.assertRaisesRegex(RuntimeError, 'Original asset changed'):
                publisher.publish(self.plan, pathlib.Path('/unused'))
            gh.assert_not_called()

    def test_concurrent_release_note_edit_is_preserved(self):
        self.release['body'] = 'New release notes edited by the owner'
        with patch.object(publisher, 'release_snapshot', return_value=self.release), \
             patch.object(publisher, 'gh') as gh:
            with self.assertRaisesRegex(RuntimeError, 'Release notes changed'):
                publisher.publish(self.plan, pathlib.Path('/unused'))
            gh.assert_not_called()

    def test_deletion_occurs_only_after_all_replacements_are_verified(self):
        events = []

        def command(*args):
            if args[:2] == ('release', 'upload'):
                self.release['assets'].append({'name': pathlib.Path(args[3]).name, 'id': 9000})
            elif args[:2] == ('release', 'edit'):
                self.release['body'] = publisher.NOTES_FILE.read_text()
                events.append('notes')
            elif args[:3] == ('api', '--method', 'DELETE'):
                events.append('delete')
                self.release['assets'] = [a for a in self.release['assets'] if a['id'] != self.jar['id']]
            return b''

        def verified(plan, output, names, directory):
            events.append('verified ' + str(len(names)))

        with patch.object(publisher, 'release_snapshot', side_effect=lambda _: copy.deepcopy(self.release)), \
             patch.object(publisher, 'verify_uploaded', side_effect=verified), \
             patch.object(publisher, 'gh', side_effect=command):
            publisher.publish(self.plan, pathlib.Path('/unused'))
        self.assertEqual(events, ['verified 3', 'verified 1', 'notes', 'delete'])

    def test_verification_downloads_the_exact_asset_id(self):
        with tempfile.TemporaryDirectory() as directory:
            output = pathlib.Path(directory) / 'output'
            verification = pathlib.Path(directory) / 'verification'
            output.mkdir()
            verification.mkdir()
            path = output / 'SHA256SUMS.txt'
            path.write_bytes(b'new checksum content\n')
            asset = {'id': 123, 'name': path.name, 'size': path.stat().st_size,
                     'browser_download_url': 'https://example.invalid/cached-old-file'}

            def exact_download(repository, actual_asset, destination):
                self.assertEqual(actual_asset['id'], 123)
                destination.write_bytes(path.read_bytes())

            with patch.object(publisher, 'release_snapshot', return_value={'assets': [asset]}), \
                 patch.object(publisher, 'download_asset', side_effect=exact_download) as download, \
                 patch.object(publisher, 'download') as cached_download:
                publisher.verify_uploaded(self.plan, output, [path.name], verification)
                download.assert_called_once()
                cached_download.assert_not_called()


if __name__ == '__main__':
    unittest.main()
