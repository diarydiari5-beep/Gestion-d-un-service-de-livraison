<
param(
    [string]$BaseUrl = "http://localhost:8084",
    [string]$Utilisateur = "admin",
    [string]$MotDePasse = "admin123",
    [int]$IntervalleSecondes = 2,
    [double]$VitesseKmh = 30,
    [int]$RafraichirGeometrie = 15,
    [switch]$UneSeuleFois
)

$ErrorActionPreference = "Stop"


$jeton = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$Utilisateur`:$MotDePasse"))
$entetes = @{ Authorization = "Basic $jeton" }

function Get-DistanceMetres {
    param([double]$Lat1, [double]$Lon1, [double]$Lat2, [double]$Lon2)
    $dy = ($Lat1 - $Lat2) * 111320.0
    $dx = ($Lon1 - $Lon2) * 105000.0
    return [Math]::Sqrt(($dx * $dx) + ($dy * $dy))
}


function Get-StepSuivant {
    param($Points, [double]$Lat, [double]$Lon, [double]$PasMetres)

    $index = 0
    $minimum = [double]::MaxValue
    for ($i = 0; $i -lt $Points.Count; $i++) {
        $distance = Get-DistanceMetres $Lat $Lon $Points[$i][0] $Points[$i][1]
        if ($distance -lt $minimum) {
            $minimum = $distance
            $index = $i
        }
    }

    $reste = $PasMetres
    for ($i = $index; $i -lt $Points.Count - 1; $i++) {
        $aLat = $Points[$i][0]
        $aLon = $Points[$i][1]
        $bLat = $Points[$i + 1][0]
        $bLon = $Points[$i + 1][1]
        $segment = Get-DistanceMetres $aLat $aLon $bLat $bLon
        if ($segment -le 0.0) { continue }
        if ($segment -ge $reste) {
            $ratio = $reste / $segment
            return @{
                Lat     = $aLat + (($bLat - $aLat) * $ratio)
                Lon     = $aLon + (($bLon - $aLon) * $ratio)
                Reste   = $true
                Ecart   = $minimum
            }
        }
        $reste -= $segment
    }

    $dernier = $Points[$Points.Count - 1]
    return @{
        Lat   = [double]$dernier[0]
        Lon   = [double]$dernier[1]
        Reste = $false
        Ecart = $minimum
    }
}

function Get-StatutActif {
    param([string]$Statut)
    return @("ASSIGNEE", "PRISE_EN_CHARGE", "EN_TRANSIT") -contains ($Statut + "").ToUpper()
}

function Get-VersCible {
    param([string]$Statut)
    if (($Statut + "").ToUpper() -eq "ASSIGNEE") { return "enlevement" }
    return "livraison"
}

# Recupere l'itineraire brut (sans le prefixe de depart) pour la phase demandee.
function Get-Geometrie {
    param([long]$CommandeId, [string]$Vers, [double]$Lat, [double]$Lon)

    $url = "$BaseUrl/api/commandes/$CommandeId/itineraire?vers=$Vers"
    try {
        $route = Invoke-RestMethod -Uri $url -Method Get -Headers $entetes -TimeoutSec 15
    }
    catch {
        return $null
    }
    if (-not $route -or -not $route.points -or $route.points.Count -lt 2) {
        return $null
    }

    $points = @()
    foreach ($p in $route.points) {
        if ($p -and $p.Count -ge 2) {
            $points += , @([double]$p[0], [double]$p[1])
        }
    }
    if ($points.Count -lt 2) { return $null }

    
    if ($points.Count -ge 3) {
        $points = $points[1..($points.Count - 1)]
    } elseif ((Get-DistanceMetres $Lat $Lon $points[0][0] $points[0][1]) -gt 60.0) {
        $points = @(@($Lat, $Lon)) + $points
    }
    return , $points
}

$geometries = @{}

function Get-GeometrieMiseEnCache {
    param([long]$CommandeId, [string]$Vers, [double]$Lat, [double]$Lon, [int]$Tick)

    $cle = "$CommandeId/$Vers"
    $entree = $geometries[$cle]
    if ($entree -and ($Tick -lt $entree.Tick + $RafraichirGeometrie)) {
        return $entree.Points
    }
    $points = Get-Geometrie -CommandeId $CommandeId -Vers $Vers -Lat $Lat -Lon $Lon
    $geometries[$cle] = @{ Points = $points; Tick = $Tick }
    if (-not $points) {
        $geometries.Remove($cle)
    }
    return $points
}

$livreurs = Invoke-RestMethod -Uri "$BaseUrl/api/livreurs/positions" -Method Get -Headers $entetes -TimeoutSec 15
if (-not $livreurs) {
    Write-Host "Aucun livreur enregistre. Lancez d'abord scripts/reset-demo.sql." -ForegroundColor Red
    exit 1
}

$etat = @{}
foreach ($l in $livreurs) {
    if ($null -ne $l.latitude -and $null -ne $l.longitude) {
        $etat[[long]$l.id] = @{
            Lat     = [double]$l.latitude
            Lon     = [double]$l.longitude
            Nom     = ("{0} {1}" -f $l.prenom, $l.nom).Trim()
            Depart  = [string]::IsNullOrWhiteSpace($l.zoneActivite)
        }
    }
}
if ($etat.Count -eq 0) {
    Write-Host "Aucun livreur avec position GPS." -ForegroundColor Red
    exit 1
}

$pasMetres = ($VitesseKmh * 1000.0 / 3600.0) * $IntervalleSecondes
Write-Host "Simulation de flotte sur $BaseUrl : $($etat.Count) livreur(s), $VitesseKmh km/h, un pas toutes les $IntervalleSecondes s ($([Math]::Round($pasMetres)) m)." -ForegroundColor Green
Write-Host "ASSIGNEE -> enlèvement, PRISE_EN_CHARGE / EN_TRANSIT -> livraison. Ctrl+C pour arreter." -ForegroundColor Green

$tick = 0
$annonces = @{}
try {
    while ($true) {
        $commandes = @()
        try {
            # Ne pas encapsuler dans @() : PowerShell 5.1 replie alors le tableau
            # JSON en un seul objet dont les proprietes sont des tableaux.
            $commandes = Invoke-RestMethod -Uri "$BaseUrl/api/commandes/positions" -Method Get -Headers $entetes -TimeoutSec 15
        }
        catch {
            Write-Host "Positions des commandes indisponibles : $($_.Exception.Message)" -ForegroundColor Yellow
        }

        $actives = @{}
        foreach ($c in $commandes) {
            if ($null -eq $c.livreurId) { continue }
            if (-not (Get-StatutActif $c.statut)) { continue }
            $cle = [long]$c.livreurId
            if (-not $actives.ContainsKey($cle)) {
                $actives[$cle] = @()
            }
            $actives[$cle] += $c
        }

        foreach ($cle in @($etat.Keys)) {
            $s = $etat[$cle]
            $s.Depart = $false
            if (-not $actives.ContainsKey($cle)) {
                continue
            }

            $commande = $actives[$cle][0]
            $vers = Get-VersCible $commande.statut
            $cibleLat = if ($vers -eq "enlevement") { $commande.latitudeEnlevement } else { $commande.latitudeLivraison }
            $cibleLon = if ($vers -eq "enlevement") { $commande.longitudeEnlevement } else { $commande.longitudeLivraison }
            if ($null -eq $cibleLat -or $null -eq $cibleLon) {
                Write-Host "Commande $($commande.numero) sans coordonnees de $vers : deplacement impossible." -ForegroundColor Yellow
                continue
            }

            $distanceCible = Get-DistanceMetres $s.Lat $s.Lon ([double]$cibleLat) ([double]$cibleLon)
            if ($distanceCible -le 25.0) {
                if ($annonces["$cle/$vers"] -ne $true) {
                    $annonces["$cle/$vers"] = $true
                    Write-Host "  [arrivee] $($s.Nom) est a 25 m du point de $vers de $($commande.numero)." -ForegroundColor Cyan
                }
                continue
            }
            $annonces.Remove("$cle/$vers")

            $points = Get-GeometrieMiseEnCache -CommandeId ([long]$commande.id) -Vers $vers -Lat $s.Lat -Lon $s.Lon -Tick $tick
            if (-not $points) {
                $points = , @([double]$cibleLat, [double]$cibleLon)
            }

            $step = Get-StepSuivant -Points $points -Lat $s.Lat -Lon $s.Lon -PasMetres $pasMetres
            $nouvelleLat = $step.Lat
            $nouvelleLon = $step.Lon
            if ((Get-DistanceMetres $nouvelleLat $nouvelleLon ([double]$cibleLat) ([double]$cibleLon)) -lt $pasMetres) {
                $nouvelleLat = [double]$cibleLat
                $nouvelleLon = [double]$cibleLon
            }

            $s.Lat = $nouvelleLat
            $s.Lon = $nouvelleLon
            $s.Depart = $true
            $etat[$cle] = $s

            $corps = @{ latitude = $nouvelleLat; longitude = $nouvelleLon } | ConvertTo-Json
            try {
                Invoke-RestMethod -Uri "$BaseUrl/api/livreurs/$cle/position" -Method Post -Headers $entetes -ContentType "application/json" -Body $corps -TimeoutSec 15 | Out-Null
            }
            catch {
                Write-Host "  Position de $($s.Nom) refusee : $($_.Exception.Message)" -ForegroundColor Yellow
            }
        }

        $enMouvement = @($etat.Keys | Where-Object { $etat[$_].Depart }).Count
        Write-Host ("Positions publiees : $enMouvement/$($etat.Count) livreur(s) en deplacement - " + (Get-Date -Format "HH:mm:ss"))

        $tick++
        if ($UneSeuleFois) { break }
        Start-Sleep -Seconds $IntervalleSecondes
    }
}
finally {
    Write-Host "Simulation arretee." -ForegroundColor Yellow
}
