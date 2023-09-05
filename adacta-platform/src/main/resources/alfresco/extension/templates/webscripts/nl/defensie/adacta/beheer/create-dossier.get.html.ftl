<!DOCTYPE html>
<head>
<title>Creeer ADACTA Dossiers</title>
</head>

<body>
	<div style="height: 800px; width: 100%; padding: 0px; text-align: center; font-family: Verdana, Arial, sans-serif; line-height: 1.5; font-size: 72.5%;">
		<br />
		<div style="width: 500px; background-color: rgba(237, 243, 245, 1); text-align: justify; margin: auto; margin-top: 80px; padding: 20px;">
	<h1>Beheer - create dossier</h1>
	<h5 style="color:green">* deze functie verondersteld dat er een csv klaarstaat in Data dictionary/Beheer/createDossiers.</h5>
	<h5 style="color:green">* De naam van dit bestand kan hieronder opgegeven worden.</h5>
	<h5 style="color:green">* Deze csv heeft de volgende indeling: dossiernaam;applid;mrn;naam;dep;dpCodes</h5>
	<h5 style="color:green">* DpCodes moet worden gescheiden door een spatie.</h5>	
	<h5 style="color:green">* De csv mag geen kolomheaders hebben.</h5>
	<p><form method="GET" action="">
	<label for="csv">Naam uit te voeren csv:</label>
	<input type="text" value="" name="csv" id="csv" style="margin-left: 20px; width: 230px;" />
	<input type="submit" name="submit" value="Voer uit" style="float:right; background-color: #fff; font-size: 10px; padding: 5px; width: 60px; height: 23px; margin-top: -2px; text-align: center; vertical-align: middle;" />
	<br />
	
	</form>
	</p>
	
	<p style="color: red;">
		${msg}
	</p>
		</div>
	</div>
</body>
</html>