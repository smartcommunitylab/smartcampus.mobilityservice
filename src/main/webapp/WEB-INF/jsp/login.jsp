<!DOCTYPE html>
<html lang="en" ng-app="extranet">
	<head>
		<meta charset="utf-8">
		<meta http-equiv="X-UA-Compatible" content="IE=edge">
		<meta name="viewport" content="width=device-width, initial-scale=1">
		<meta name="description" content="">
		<meta name="author" content="">
		<meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate" />
		<meta http-equiv="Pragma" content="no-cache" />
		<meta http-equiv="Expires" content="-1" />	      
	      <title>Mobility Communicator Console Login</title>
	      <!-- Bootstrap core CSS -->
	      <link href="css/bootstrap.min.css" rel="stylesheet">
	      <!-- Custom styles for this template -->
	      <link href="css/style.css" rel="stylesheet">
	
	      <script src="lib/angular/angular.min.js"></script>
	      <script src="lib/angular/angular-route.min.js"></script>
	      <script src="lib/ui-bootstrap-tpls-0.12.1.min.js"></script>

    </head>
    <body>
      <div class="container">
        <div class="row">
          <div class="col-md-offset-4 col-md-4">
            <div class="panel panel-default">
            <h3 style="text-align:center">Mobility Console Login</h3>
            <form action="login" method="post">
                <div>&nbsp;</div>
                <div class="col-md-12 form-group"><label> Username: </label><input class="form-control" type="text" name="username"/></div>
                <div class="col-md-12 form-group"><label> Password: </label><input class="form-control" type="password" name="password"/></div>
                <div class="col-md-12 form-group"><button class="btn btn-primary"/>Sign In</button></div>
                <div>&nbsp;</div>
            </form>
            </div>
          </div>
        </div>
      </div>  
      <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
      <script src="lib/bootstrap.min.js"></script>
    </body>
</html>