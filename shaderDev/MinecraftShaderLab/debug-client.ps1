param(
    [Parameter(Mandatory=$true)][string]$Session,
    [ValidateSet('status','load','reload','configure','render','frame','shutdown')][string]$Command = 'status',
    [string]$Body = '{}',
    [string]$InputFile = '',
    [string]$Output = ''
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$labSession = [IO.File]::ReadAllText([IO.Path]::GetFullPath($Session)) | ConvertFrom-Json
if ($labSession.state -ne 'ready') { throw 'The debug session is not running.' }
$labUri = [Uri]$labSession.baseUrl
if (!$labUri.IsLoopback -or $labUri.Scheme -ne 'http') { throw 'Debug sessions must use a local HTTP endpoint.' }
if ($InputFile) { $Body = [IO.File]::ReadAllText([IO.Path]::GetFullPath($InputFile)) }
$labMethod = if ($Command -eq 'status' -or $Command -eq 'frame') { [Net.Http.HttpMethod]::Get } else { [Net.Http.HttpMethod]::Post }
$labEndpoint = if ($Command -eq 'frame') { 'frame.png' } else { $Command }
$labClient = [Net.Http.HttpClient]::new()
$labClient.Timeout = [TimeSpan]::FromSeconds(40)
$labRequest = [Net.Http.HttpRequestMessage]::new($labMethod, ($labSession.baseUrl + '/v1/' + $labEndpoint))
$labRequest.Headers.Add('X-ShaderLab-Token', $labSession.token)
if ($labMethod -eq [Net.Http.HttpMethod]::Post) {
    $null = $Body | ConvertFrom-Json
    $labRequest.Content = [Net.Http.StringContent]::new($Body, [Text.Encoding]::UTF8, 'application/json')
}
try {
    $labResponse = $labClient.SendAsync($labRequest).GetAwaiter().GetResult()
    try {
        if (!$labResponse.IsSuccessStatusCode) { throw $labResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult() }
        if ($Command -eq 'frame') {
            if (!$Output) { throw '-Output is required for a PNG frame.' }
            $labOutputPath = [IO.Path]::GetFullPath($Output)
            [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($labOutputPath)) | Out-Null
            [IO.File]::WriteAllBytes($labOutputPath, $labResponse.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult())
            Write-Output $labOutputPath
        } else {
            $labText = $labResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            if ($Output) { [IO.File]::WriteAllText([IO.Path]::GetFullPath($Output), $labText, [Text.UTF8Encoding]::new($false)) }
            Write-Output $labText
        }
    } finally { $labResponse.Dispose() }
} finally { $labRequest.Dispose(); $labClient.Dispose() }
