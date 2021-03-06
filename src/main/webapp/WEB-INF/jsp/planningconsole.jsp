<!DOCTYPE html>
<html lang="en" ng-app="webplanner">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="">

    <title>SmartPlanner</title>

    <!-- Bootstrap core CSS -->
    <link href="../css/bootstrap.min.css" rel="stylesheet">
    <!-- Custom styles for this template -->
    <link href="../css/style.css" rel="stylesheet">

    <script src="../lib/angular/angular.min.js"></script>
    <script src="../lib/angular/angular-route.min.js"></script>
    <script src="../lib/ui-bootstrap-tpls-0.12.1.min.js"></script>
    <script src="https://maps.googleapis.com/maps/api/js?libraries=geometry&v=3.exp"></script>
    <script src="../lib/sprintf.min.js"></script>

    <script src="../js/props.js"></script>
    <script src="../js/app2.js"></script>
    <script src="../js/controllers.js"></script>
    <script src="../js/services.js"></script>

    <!-- Just for debugging purposes. Don't actually copy these 2 lines! -->
    <!--[if lt IE 9]><script src="lib/ie8-responsive-file-warning.js"></script><![endif]-->
    <script src="../lib/ie-emulation-modes-warning.js"></script>

    <!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
      <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->

	<!-- <link rel="import" href="../templates/policymodals.html"> -->
  </head>

  <body>
   <div ng-view></div>
    <!-- Bootstrap core JavaScript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script>
    <script src="../lib/bootstrap.min.js"></script>
    <!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
    <script src="../lib/ie10-viewport-bug-workaround.js"></script>
    
    <script src="../lib/bootbox.min.js"></script>
  </body>
</html>
