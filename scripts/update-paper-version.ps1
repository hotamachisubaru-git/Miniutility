param(
    [Parameter(Mandatory = $true)]
    [string] $MinecraftVersion,

    [string] $Channel = "ALPHA",

    [switch] $UpdateProjectVersion,

    [string] $ProjectVersionSuffix,

    [string] $PomPath = (Join-Path $PSScriptRoot "..\pom.xml")
)

$ErrorActionPreference = "Stop"

$resolvedPomPath = Resolve-Path $PomPath
$normalizedChannel = $Channel.ToUpperInvariant()
$normalizedProjectVersionSuffix = if ([string]::IsNullOrWhiteSpace($ProjectVersionSuffix)) {
    "$($normalizedChannel.ToLowerInvariant()).1"
} else {
    $ProjectVersionSuffix
}
$channelDisplayName = switch ($normalizedChannel) {
    "STABLE" { "安定版" }
    "ALPHA" { "アルファ版" }
    "BETA" { "ベータ版" }
    default { $normalizedChannel }
}
$apiUrl = "https://fill.papermc.io/v3/projects/paper/versions/$MinecraftVersion/builds"
$headers = @{
    "User-Agent" = "Miniutility-version-updater/1.0"
}

$builds = Invoke-RestMethod -Uri $apiUrl -Headers $headers
$build = $builds |
    Where-Object { $_.channel -eq $normalizedChannel } |
    Sort-Object -Property id -Descending |
    Select-Object -First 1

if ($null -eq $build) {
    throw "Paper $MinecraftVersion の $channelDisplayName ビルドが見つかりません。"
}

$paperVersion = "$MinecraftVersion.build.$($build.id)-$($normalizedChannel.ToLowerInvariant())"

[xml] $pom = Get-Content -Raw $resolvedPomPath
$namespaceManager = [System.Xml.XmlNamespaceManager]::new($pom.NameTable)
$namespaceManager.AddNamespace("m", $pom.Project.NamespaceURI)

$project = $pom.SelectSingleNode("/m:project", $namespaceManager)
$properties = $project.SelectSingleNode("m:properties", $namespaceManager)

function Set-PomProperty {
    param(
        [xml] $Document,
        [System.Xml.XmlElement] $Properties,
        [string] $Name,
        [string] $Value
    )

    $node = $Properties.ChildNodes |
        Where-Object { $_.NodeType -eq [System.Xml.XmlNodeType]::Element -and $_.LocalName -eq $Name } |
        Select-Object -First 1

    if ($null -eq $node) {
        $node = $Document.CreateElement($Name, $Document.Project.NamespaceURI)
        [void] $Properties.AppendChild($node)
    }

    $node.InnerText = $Value
}

Set-PomProperty -Document $pom -Properties $properties -Name "java.version" -Value "25"
Set-PomProperty -Document $pom -Properties $properties -Name "minecraft.version" -Value $MinecraftVersion
Set-PomProperty -Document $pom -Properties $properties -Name "paper.version" -Value $paperVersion

if ($UpdateProjectVersion) {
    $versionNode = $project.SelectSingleNode("m:version", $namespaceManager)
    $versionNode.InnerText = "$MinecraftVersion-$normalizedProjectVersionSuffix"
}

$writerSettings = [System.Xml.XmlWriterSettings]::new()
$writerSettings.Encoding = [System.Text.UTF8Encoding]::new($false)
$writerSettings.Indent = $true

$writer = [System.Xml.XmlWriter]::Create($resolvedPomPath, $writerSettings)
try {
    $pom.Save($writer)
} finally {
    $writer.Close()
}

Write-Host "Minecraft バージョン: $MinecraftVersion"
Write-Host "Paper API バージョン: $paperVersion"
