var gamificationConsole = angular.module('gameconsole', [ 'ui.bootstrap', 'ngScrollable']);

gamificationConsole.controller('GameCtrl', function($scope, $timeout, $http) {
	$scope.users = [];
	$scope.userMap = {};
	$scope.selectedUser = null;
	$scope.selectedItinerary = null;
	$scope.selectedInstance = null;
	$scope.layers = [];
	$scope.fixpaths = false;
	$scope.removeoutliers = false;
	$scope.eventsMarkers = new Map();
	$scope.fromDate = Date.today().previous().saturday().previous().saturday();
	$scope.toDate = Date.today().next().saturday().add(-1).minute();
	$scope.openedFrom = false;
	$scope.openedTo = false;
	$scope.excludeZeroPoints = false;
	$scope.toCheck = false;
	$scope.unapprovedOnly = false;
	$scope.approvedList = [{name: 'All', value : false}, {name: 'Modified', value : true}];
	$scope.filterApproved = $scope.approvedList[0];
	$scope.scores = "";
	
	$scope.format = 'EEE MMM dd HH:mm';
	$scope.dateOptions = {
	    startingDay: 1
	};

	$timeout(function() {
		document.getElementById('fromDate').value = $scope.fromDate.toString('ddd MMM dd HH:mm');
		document.getElementById('toDate').value = $scope.toDate.toString('ddd MMM dd HH:mm');
	});

	var spinOpts = {
			  lines: 13 // The number of lines to draw
			, length: 28 // The length of each line
			, width: 14 // The line thickness
			, radius: 42 // The radius of the inner circle
			, scale: 1 // Scales overall size of the spinner
			, corners: 1 // Corner roundness (0..1)
			, color: '#000' // #rgb or #rrggbb or array of colors
			, opacity: 0.25 // Opacity of the lines
			, rotate: 0 // The rotation offset
			, direction: 1 // 1: clockwise, -1: counterclockwise
			, speed: 1 // Rounds per second
			, trail: 60 // Afterglow percentage
			, fps: 20 // Frames per second when using setTimeout() as a fallback for CSS
			, zIndex: 2e9 // The z-index (defaults to 2000000000)
			, className: 'spinner' // The CSS class to assign to the spinner
			, top: '66%' // Top position relative to parent
			, left: '50%' // Left position relative to parent
			, shadow: false // Whether to render a shadow
			, hwaccel: false // Whether to use hardware acceleration
			, position: 'absolute' // Element positioning
			}
	var target = document.getElementById('console')
	var spinner = new Spinner(spinOpts);
	
	var load = function() {
		$http.get("console/appId").success(function(data) {	
			$scope.appId = data;
			spinner.spin(target);
			$http.get("console/users?fromDate=" + $scope.fromDate.getTime() + "&toDate=" + $scope.toDate.getTime() + "&excludeZeroPoints=" + $scope.excludeZeroPoints + "&unapprovedOnly=" + $scope.unapprovedOnly + "&toCheck=" + $scope.toCheck, {"headers" : { "appId" : $scope.appId}}).then(function(data) {
				var users = [];
				$scope.userTotals = {};
				data.data.forEach(function(descr) {
					users.push(descr.userId);
					$scope.userTotals[descr.userId] = {
						"total" : descr.total,
						"failed" : (descr.total - descr.valid)
					};
				});

				$scope.users = users;
				$scope.userMap = {};
				spinner.stop();
			});			
		});
	}

	load();

	$scope.selectUser = function(user) {
		if ($scope.selectedUser == user)
			$scope.selectedUser = null;
		else {
			$scope.selectedUser = user;

			if (!$scope.userMap[user]) {
				spinner.spin(target);
				$http.get("console/useritinerary/" + user + "?fromDate=" + $scope.fromDate.getTime() + "&toDate=" + $scope.toDate.getTime() + "&excludeZeroPoints=" + $scope.excludeZeroPoints + "&unapprovedOnly=" + $scope.unapprovedOnly + "&toCheck=" + $scope.toCheck, {"headers" : { "appId" : $scope.appId}}).then(function(data) {
					$scope.userMap[user] = data.data;
					spinner.stop();
				});
			}

		}
		$scope.selectedItinerary = null;
		$scope.selectedInstance = null;
		resetLayers();
	}

	$scope.selectItinerary = function(itinerary) {
		resetLayers();
		$scope.selectedInstance = null;
		$scope.selectedItinerary = itinerary;
//		itinerary.instances.sort(function(a, b) {
//			if (!a.day && !b.day)
//				return 0;
//			if (!a.day)
//				return -1;
//			if (!b.day)
//				return 1;
//			return a.day.localeCompare(b.day);
//		});
		
		// if (itinerary.instances.length == 1) {
		// $scope.selectInstance(itinerary.instances[0]);
		// }
	}

	$scope.validColor = function(totals) {
		var r = 127 + Math.floor(128 * Math.pow(totals.failed / totals.total, 1.5));
		var g = 0 + Math.floor(255 * ((totals.total - totals.failed) / totals.total));
		return "color:rgb(" + r + "," + g + "," + 64 + ")";
	}

	var resetLayers = function() {
		if (!$scope.layers)
			return;

		$scope.layers.forEach(function(l) {
			l.setMap(null);
		});
		$scope.layers = [];
	}

	$scope.revalidate = function() {
		spinner.spin(target);
		$http.post("console/validate", {}, {"headers" : { "appId" : $scope.appId}}).then(function(data) {
			load();
			spinner.stop();
		});
	}
	
	$scope.switchCurrentValidity = function(toggle) {
		if (toggle) {
		$http.post("console/itinerary/switchValidity/" + $scope.selectedInstance.id + "?value=" + $scope.selectedInstance.switchValidity, {}, {"headers" : { "appId" : $scope.appId}}).then(function(data) {
			$scope.selectedInstance.switchValidity = data.data.switchValidity;
			$scope.reselectInstance();
		});
		} else {
			$scope.selectedInstance.switchValidity = !$scope.selectedInstance.switchValidity;
		}
	}	

	
	$scope.toggleToCheck = function(instance) {
		$http.post("console/itinerary/toCheck/" + instance.id + "?value=" + !instance.toCheck, {}, {"headers" : { "appId" : $scope.appId}}).then(function(data) {
			instance.toCheck = data.data.toCheck;
		});
	}			
	
	$scope.toggleApproved = function(instance) {
		$http.post("console/itinerary/approve/" + instance.id + "?value=" + !instance.approved, {}, {"headers" : { "appId" : $scope.appId}}).then(function(data) {
			instance.approved = data.data.approved;
		});
	}		
	
	$scope.approveAll = function() {
//		spinner.spin(target);		
		$http.post("console/approveFiltered?fromDate=" + $scope.fromDate.getTime() + "&toDate=" + $scope.toDate.getTime() + "&excludeZeroPoints=" + $scope.excludeZeroPoints + "&toCheck=" + $scope.toCheck, {"headers" : { "appId" : $scope.appId}}).then(function(data) {
			load();
			spinner.stop();
		});
	}
	
	$scope.report = function() {
		spinner.spin(target);
		$http.get("console/report?fromDate=" + $scope.fromDate.getTime() + "&toDate=" + $scope.toDate.getTime(), {"headers" : { "appId" : $scope.appId}}).then(function(data) {
			load();
			spinner.stop();
		});
	}	
	
	$scope.getValidityStyle = function(instance) {
		if ((instance.valid & !instance.switchValidity) | (!instance.valid & instance.switchValidity)) {
			return true;
		}
		if ((!instance.valid & !instance.switchValidity) | (instance.valid & instance.switchValidity)) {
			return false;
		}
	}
	
	$scope.reload = function() {
		load();
	}	

	$scope.reselectInstance = function() {
		if ($scope.selectedInstance != null) {
			$scope.selectInstance($scope.selectedInstance);
		}
		
	}
	
	$scope.showAllPoints = function() {
		for (var i = 0; i < $scope.selectedInstance.geolocationEvents.length; i++) {
			var e = $scope.selectedInstance.geolocationEvents[i];
			var pos = {'lat' : e.latitude, 'lng' : e.longitude};	
			var s = e.latitude + "_" + e.longitude;
			var m = $scope.createMarkerObject(pos, 'circle2', i + 1, true);
			$scope.layers.push(m);
//			$scope.eventsMarkers.set(s,m);
		}
	}	
	
	
	
	$scope.newEventMarker = function(lat,lng, i) {
		var pos = {'lat' : lat, 'lng' : lng};	
		var s = lat + "_" + lng;
		
		if (!$scope.eventsMarkers.has(s)) {
			var m = $scope.createMarkerObject(pos, 'circle', i);
			$scope.layers.push(m);
			$scope.eventsMarkers.set(s,m);
		} else {
			var m = $scope.eventsMarkers.get(s);
			m.setMap(null);
			$scope.eventsMarkers.delete(s);
		}
	}
	
	$scope.selectInstance = function(instance) {
		$scope.selectedInstance = instance;
		eventsMarkers = new Map();

		resetLayers();

		// SHOW TRACKED DATA
		var coordinates = [];
		instance.geolocationEvents.sort(function(a, b) {
			return a.recorded_at - b.recorded_at;
		});
		var bounds = new google.maps.LatLngBounds();
		instance.legs = [];
		var lastLeg = {
			activity_type : null
		};
		
		var cx = 0;
		var cy = 0;
		
		instance.geolocationEvents.forEach(function(e) {
			cx += e.longitude;
			cy += e.latitude;
			
			var p = {
				lat : e.latitude,
				lng : e.longitude,
				acc: e.accuracy,
				recorded_at : e.recorded_at
			};
			coordinates.push(p);
			bounds.extend(new google.maps.LatLng(p.lat, p.lng));
			var type = e.activity_type;
			if (type != lastLeg.activity_type && type != 'unknown') {
				var leg = angular.copy(e);
				leg.count = 1;
				instance.legs.push(leg);
				lastLeg = leg;
			} else if (type == 'unknown') {
				var leg = angular.copy(e);
				leg.count = 1;
				instance.legs.push(leg);
			} else {
				lastLeg.count++;
				lastLeg.recorded_till = e.recorded_at;
			}
		});

		// coordinates.splice(0,1);
		// coordinates.splice(coordinates.length-1,1);

		if (instance.freeTrackingTransport) {
			$scope.scores = "[" + computeFreeTrackingScore(coordinates, instance.freeTrackingTransport);
		}
		if ($scope.fixpaths) {
			coordinates = removeOutliers(coordinates);
			coordinates = transform(coordinates);
			
			if (instance.freeTrackingTransport) {
				$scope.scores += "," + computeFreeTrackingScore(coordinates, instance.freeTrackingTransport) + "]";
			}			
		} else {
			$scope.scores += ",??]";
		}		

		if (!instance.freeTrackingTransport) {
			$scope.scores = "";
		}

		$scope.map.fitBounds(bounds);
		
		newMarker(coordinates[0], 'ic_start');
		newMarker(coordinates[coordinates.length - 1], 'ic_stop');


		var path = new google.maps.Polyline({
			path : coordinates,
			geodesic : true,
			strokeColor : 'blue',
			strokeOpacity : 1.0,
			strokeWeight : 2
		});
		$scope.layers.push(path);
		path.setMap($scope.map);

		// $SHOW PLANNED DATA
		if (instance.itinerary != null) {
			instance.itinerary.data.leg.forEach(function(leg) {
				var path = google.maps.geometry.encoding.decodePath(leg.legGeometery.points);
				var line = new google.maps.Polyline({
					path : path,
					strokeColor : 'green',
					strokeOpacity : 0.8,
					strokeWeight : 2,
					map : $scope.map
				});
				newMarker(path[0], 'step');
				$scope.layers.push(line);
			});
		}
	}

	var newMarker = function(pos, icon, i) {
		var m = $scope.createMarkerObject(pos, icon, i);
		$scope.layers.push(m);
		return m;
	};
	
	$scope.createMarkerObject = function(pos, icon, i, bottom) {
		var m;
		if (i) {
			m = new google.maps.Marker({
			position : pos,
			icon : '../img/' + icon + '.png',
			label : { text : "" + i, fontWeight : "bold", fontSize : "22px", color: "DarkRed"},
			map : $scope.map,
			draggable : false,
			labelContent : "A",
			labelAnchor : new google.maps.Point(3, 30),
			labelClass : "labels",
			zIndex : (10 + i + bottom ? 0 : 100),
			draggable: true
			});
		} else {
			m = new google.maps.Marker({
				position : pos,
				icon : '../img/' + icon + '.png',
				map : $scope.map,
				draggable : false,
				labelContent : "A",
				labelAnchor : new google.maps.Point(3, 30),
				labelClass : "labels",
				zIndex : 0
		});
		}
		return m;
	}
	
    if (typeof (Number.prototype.toRad) === "undefined") {
        Number.prototype.toRad = function () {
            return this * Math.PI / 180;
        }
    }
	
	function dist(p1, p2) {
        var pt1 = [p1.lat, p1.lng];
        var pt2 = [p2.lat, p2.lng];
    
        var d = false;
        if (pt1 && pt1[0] && pt1[1] && pt2 && pt2[0] && pt2[1]) {
            var lat1 = Number(pt1[0]);
            var lon1 = Number(pt1[1]);
            var lat2 = Number(pt2[0]);
            var lon2 = Number(pt2[1]);

            var R = 6371; // km
            //var R = 3958.76; // miles
            var dLat = (lat2 - lat1).toRad();
            var dLon = (lon2 - lon1).toRad();
            var lat1 = lat1.toRad();
            var lat2 = lat2.toRad();
            var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
            var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            d = R * c;
        } else {
            console.log('cannot calculate distance!');
        }
        return d;
  }
  
  function transform(array) {
    var res = [];
    for (var i = 1; i < array.length; i++) {
      transformPair(array[i-1], array[i], res, dist);
    }
    return res;
  }
  
  function compute(v1, a1, v2, a2, d) {
    if ((a1 + a2)/1000 > d) {
      var v = a1 > a2 ? (v2 - (v2-v1)*a2/a1) : (v1+ (v2-v1)*a1/a2);
      return [v,v];   
    }
    return [v1 + (v2-v1)*a1/d/1000, v2 - (v2-v1)*a2/d/1000];
  }
  
  function computeLats(p1, p2, d) {
    if (p1.lat > p2.lat) {
      var res = computeLats(p2,p1, d);
      return [res[1],res[0]];
    }
    return compute(p1.lat, p1.acc, p2.lat, p2.acc, d);
  }
  function computeLngs(p1, p2, d) {
    if (p1.lng > p2.lng) {
      var res = computeLngs(p2, p1, d);
      return [res[1],res[0]];
    }
    return compute(p1.lng, p1.acc, p2.lng, p2.acc, d);
  }
  
  function transformPair(p1, p2, res, distFunc) {
    var d = distFunc(p1,p2);
	if (d != 0) {
		var lats = computeLats(p1,p2,d);
		var lngs = computeLngs(p1,p2,d);
		res.push({lat: lats[0], lng: lngs[0], acc: (p1.acc + p2.acc) / 2, recorded_at: (p1.recorded_at + p2.recorded_at) / 2});
		res.push({lat: lats[1], lng: lngs[1], acc: (p1.acc + p2.acc) / 2, recorded_at: (p1.recorded_at + p2.recorded_at) / 2});
	}
  }	
	
  function removeOutliers(array) {
	  
	  var distance = 0;
	  var itArray = array;
	  var res = [];
	  var removed = true;
	  
	  for (var i = 1; i < itArray.length; i++) {
  		var d = dist(itArray[i], itArray[i - 1]) * 1000;
  		t = itArray[i].recorded_at - itArray[i - 1].recorded_at;
  		if (t > 0) {
  			distance += d;
  		}
	  }
	  var t = (itArray[itArray.length - 1].recorded_at - itArray[0].recorded_at) / 1000;
	  var avgSpeed = (3.6 * distance /  t);
	  
	  var toRemove = [];
//	  console.log(">" + itArray.length);

	  	res = [];
	  	toRemove = [];
	    for (var i = 1; i < itArray.length - 1; i++) {
	    		var d1 = dist(itArray[i], itArray[i - 1]) * 1000;
	    		var t1 = (itArray[i].recorded_at - itArray[i - 1].recorded_at) / 1000;
	    		var s1 = 0;
	    		if (t1 > 0) {
	    			s1 = (3.6 * d1 /  t1);
	    		}
	    		var d2 = dist(itArray[i], itArray[i + 1]) * 1000;
	    		var t2 = (itArray[i + 1].recorded_at - itArray[i].recorded_at) / 1000;
	    		var s2 = 0;
	    		if (t2 > 0) {
	    			s2 = (3.6 * d2 /  t2);
	    		}	    		
    			
	    		var index = null;
	    		
	    		var d3 = dist(itArray[i - 1], itArray[i + 1]) * 1000;
	    		
	    		var a = Math.acos((d1 * d1 + d2 * d2 - d3 * d3) / (2 * d1 * d2));
	    		
// console.log("\t" + (i + 1) + ": " + a + " <- " + rd1 + "," + rd2 + "," +
// rd3);
	    		
//	    		console.log("@" + i + "/" + (itArray.length) + " -> " + s2 + "/" + avgSpeed);
	    		
	    		if (a < 0.017453292519943 * 3) {
	    			index = i;
	    		}		    		
	    		
				if (a < 0.017453292519943 * 30 && s1 > 4 * avgSpeed && s2 > 4 * avgSpeed && i != 1 && i != itArray.length - 2) {
					index = i;
				} else if (s1 > 4 * avgSpeed && i == 1) {
					index = 0;
				} else if (s2 > 4 * avgSpeed && i == itArray.length - 2) {
					index = itArray.length - 1;
				}	    		
				
				if (index) {
//					console.log(index);
					toRemove.push(index);
				}    			
    			
	    }
	    
	    for (var i = 0; i < itArray.length; i++) {
	    	if (toRemove.indexOf(i) == -1) {
	    		res.push(itArray[i]);
	    	}
	    }	    
	    
	    itArray = res;
//	    console.log("<" + res.length);

	    return res;
	  
  }
  
  function computeFreeTrackingScore(array, ttype) {
	  var distance = 0;
	  var score = 0.0;
  
	    for (var i = 1; i < array.length; i++) {
    		var d = dist(array[i], array[i - 1]);
    		distance += d;
	    }
	    
	    if (ttype == "walk") {
	    	score = (distance < 0.25 ? 0 : Math.min(3.5, distance)) * 10;
	    } else if (ttype == "bike") {
	    	score = Math.min(7, distance) * 5;
		}
	    
	    score *= 1.5;
	    
	    return parseInt(score);
  }
	
	$scope.toggleOpen = function($event, $from) {
		$event.preventDefault();
		$event.stopPropagation();

		if ($from) {
			$scope.openedFrom = !$scope.openedFrom;
			$scope.openedTo = false;
		} else {
			$scope.openedTo = !$scope.openedTo;
			$scope.openedFrom = false;
		}
	};	
	
	$scope.initMap = function() {
		document.getElementById("left-scrollable").style.height = (window.innerHeight - 283) + "px";
//		document.getElementById("right-scrollable").style.height = (window.innerHeight / 2 - 60) + "px";	
		if (!document.getElementById('map'))
			return;
		var ll = null;
		var mapOptions = null;
		ll = {
			lat : 46.073769,
			lng : 11.125985
		};
		mapOptions = {
			zoom : 15,
			center : ll
		}
		$scope.map = new google.maps.Map(document.getElementById('map'), mapOptions);
	}

	$scope.initMap();
}).directive('toggle', function() {
	return {
		span : function(scope, element, attrs) {
			if (attrs.toggle == "tooltip") {
				$(element).tooltip();
			}
			if (attrs.toggle == "popover") {
				$(element).popover();
			}
		}
	};
})
