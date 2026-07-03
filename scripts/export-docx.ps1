param(
    [Parameter(Mandatory = $true)]
    [string]$SourceMarkdown,

    [Parameter(Mandatory = $true)]
    [string]$OutputDocx
)

$ErrorActionPreference = "Stop"

function Escape-XmlText {
    param([string]$Text)
    if ($null -eq $Text) { return "" }
    return $Text.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;").Replace('"', "&quot;").Replace("'", "&apos;")
}

function New-RunXml {
    param(
        [string]$Text,
        [switch]$Bold,
        [switch]$Italic,
        [string]$Font = "Microsoft YaHei",
        [int]$Size = 22
    )

    $escaped = Escape-XmlText $Text
    $props = "<w:rFonts w:ascii=""$Font"" w:hAnsi=""$Font"" w:eastAsia=""$Font""/><w:sz w:val=""$Size""/><w:szCs w:val=""$Size""/>"
    if ($Bold) { $props += "<w:b/><w:bCs/>" }
    if ($Italic) { $props += "<w:i/><w:iCs/>" }
    return "<w:r><w:rPr>$props</w:rPr><w:t xml:space=""preserve"">$escaped</w:t></w:r>"
}

function New-ParagraphXml {
    param(
        [string]$Text,
        [string]$Style = "Normal",
        [switch]$Code
    )

    $styleXml = ""
    $spacing = "<w:spacing w:before=""80"" w:after=""80"" w:line=""320"" w:lineRule=""auto""/>"
    $indent = ""
    $border = ""
    if ($Style -ne "Normal") {
        $styleXml = "<w:pStyle w:val=""$Style""/>"
    }
    if ($Style -eq "Bullet") {
        $indent = "<w:ind w:left=""420"" w:hanging=""180""/>"
    }
    if ($Code) {
        $styleXml = ""
        $spacing = "<w:spacing w:before=""60"" w:after=""60""/>"
        $indent = "<w:ind w:left=""360""/>"
        $border = "<w:shd w:val=""clear"" w:color=""auto"" w:fill=""F5F7FA""/>"
    }

    $font = "Microsoft YaHei"
    $size = 22
    if ($Code) {
        $font = "Consolas"
        $size = 19
    }

    $run = New-RunXml -Text $Text -Font $font -Size $size
    return "<w:p><w:pPr>$styleXml$spacing$indent$border</w:pPr>$run</w:p>"
}

function New-HeadingXml {
    param([string]$Text, [int]$Level)
    $style = if ($Level -le 1) { "Heading1" } elseif ($Level -eq 2) { "Heading2" } else { "Heading3" }
    $headingSize = 24
    if ($Level -le 1) {
        $headingSize = 32
    } elseif ($Level -eq 2) {
        $headingSize = 28
    }
    $run = New-RunXml -Text $Text -Bold -Size $headingSize
    return "<w:p><w:pPr><w:pStyle w:val=""$style""/><w:spacing w:before=""260"" w:after=""160""/></w:pPr>$run</w:p>"
}

$sourcePath = (Resolve-Path $SourceMarkdown).Path
$outputPath = [System.IO.Path]::GetFullPath($OutputDocx)
$outputDir = Split-Path $outputPath -Parent
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

$utf8 = [System.Text.Encoding]::UTF8
$lines = [System.IO.File]::ReadAllLines($sourcePath, $utf8)
$body = New-Object System.Collections.Generic.List[string]
$inCode = $false

foreach ($line in $lines) {
    if ($line.StartsWith('```')) {
        $inCode = -not $inCode
        continue
    }

    if ($inCode) {
        $body.Add((New-ParagraphXml -Text $line -Code))
        continue
    }

    if ([string]::IsNullOrWhiteSpace($line)) {
        $body.Add("<w:p/>")
        continue
    }

    if ($line.StartsWith("# ")) {
        $body.Add((New-HeadingXml -Text $line.Substring(2) -Level 1))
    } elseif ($line.StartsWith("## ")) {
        $body.Add((New-HeadingXml -Text $line.Substring(3) -Level 2))
    } elseif ($line.StartsWith("### ")) {
        $body.Add((New-HeadingXml -Text $line.Substring(4) -Level 3))
    } elseif ($line.StartsWith("- ")) {
        $body.Add((New-ParagraphXml -Text ("- " + $line.Substring(2)) -Style "Bullet"))
    } else {
        $body.Add((New-ParagraphXml -Text $line))
    }
}

$tmpRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("docx-export-" + [guid]::NewGuid().ToString("N"))
$wordDir = Join-Path $tmpRoot "word"
$relsDir = Join-Path $tmpRoot "_rels"
$wordRelsDir = Join-Path $wordDir "_rels"
New-Item -ItemType Directory -Force -Path $wordDir, $relsDir, $wordRelsDir | Out-Null

$contentTypes = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>
'@

$packageRels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>
'@

$documentRels = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>
'@

$styles = @'
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Microsoft YaHei" w:hAnsi="Microsoft YaHei" w:eastAsia="Microsoft YaHei"/>
        <w:sz w:val="22"/>
        <w:szCs w:val="22"/>
      </w:rPr>
    </w:rPrDefault>
  </w:docDefaults>
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
    <w:qFormat/>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:basedOn w:val="Normal"/>
    <w:next w:val="Normal"/>
    <w:qFormat/>
    <w:pPr><w:outlineLvl w:val="0"/></w:pPr>
    <w:rPr><w:b/><w:bCs/><w:color w:val="111827"/><w:sz w:val="32"/><w:szCs w:val="32"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading2">
    <w:name w:val="heading 2"/>
    <w:basedOn w:val="Normal"/>
    <w:next w:val="Normal"/>
    <w:qFormat/>
    <w:pPr><w:outlineLvl w:val="1"/></w:pPr>
    <w:rPr><w:b/><w:bCs/><w:color w:val="1F2937"/><w:sz w:val="28"/><w:szCs w:val="28"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading3">
    <w:name w:val="heading 3"/>
    <w:basedOn w:val="Normal"/>
    <w:next w:val="Normal"/>
    <w:qFormat/>
    <w:pPr><w:outlineLvl w:val="2"/></w:pPr>
    <w:rPr><w:b/><w:bCs/><w:color w:val="374151"/><w:sz w:val="24"/><w:szCs w:val="24"/></w:rPr>
  </w:style>
</w:styles>
'@

$documentBody = [string]::Join("`n", $body)
$document = @"
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:wpc="http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas" xmlns:mc="http://schemas.openxmlformats.org/markup-compatibility/2006" xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:m="http://schemas.openxmlformats.org/officeDocument/2006/math" xmlns:v="urn:schemas-microsoft-com:vml" xmlns:wp14="http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:w10="urn:schemas-microsoft-com:office:word" xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:w14="http://schemas.microsoft.com/office/word/2010/wordml" xmlns:wpg="http://schemas.microsoft.com/office/word/2010/wordprocessingGroup" xmlns:wpi="http://schemas.microsoft.com/office/word/2010/wordprocessingInk" xmlns:wne="http://schemas.microsoft.com/office/word/2006/wordml" xmlns:wps="http://schemas.microsoft.com/office/word/2010/wordprocessingShape" mc:Ignorable="w14 wp14">
  <w:body>
$documentBody
    <w:sectPr>
      <w:pgSz w:w="11906" w:h="16838"/>
      <w:pgMar w:top="1440" w:right="1080" w:bottom="1440" w:left="1080" w:header="720" w:footer="720" w:gutter="0"/>
    </w:sectPr>
  </w:body>
</w:document>
"@

[System.IO.File]::WriteAllText((Join-Path $tmpRoot "[Content_Types].xml"), $contentTypes, $utf8)
[System.IO.File]::WriteAllText((Join-Path $relsDir ".rels"), $packageRels, $utf8)
[System.IO.File]::WriteAllText((Join-Path $wordRelsDir "document.xml.rels"), $documentRels, $utf8)
[System.IO.File]::WriteAllText((Join-Path $wordDir "styles.xml"), $styles, $utf8)
[System.IO.File]::WriteAllText((Join-Path $wordDir "document.xml"), $document, $utf8)

if (Test-Path $outputPath) {
    Remove-Item -LiteralPath $outputPath -Force
}

$zipPath = Join-Path ([System.IO.Path]::GetTempPath()) ("docx-output-" + [guid]::NewGuid().ToString("N") + ".zip")
if (Test-Path $zipPath) {
    Remove-Item -LiteralPath $zipPath -Force
}

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::Open($zipPath, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    Get-ChildItem -LiteralPath $tmpRoot -Recurse -File | ForEach-Object {
        $relativeName = $_.FullName.Substring($tmpRoot.Length).TrimStart('\', '/').Replace('\', '/')
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $archive,
            $_.FullName,
            $relativeName,
            [System.IO.Compression.CompressionLevel]::Optimal
        ) | Out-Null
    }
} finally {
    $archive.Dispose()
}
Copy-Item -LiteralPath $zipPath -Destination $outputPath -Force
Remove-Item -LiteralPath $zipPath -Force
Remove-Item -LiteralPath $tmpRoot -Recurse -Force

Write-Host "DOCX exported: $outputPath"
