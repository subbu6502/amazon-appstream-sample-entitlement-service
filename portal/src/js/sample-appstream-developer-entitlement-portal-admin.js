var app = angular.module('portal', []);

app.controller("NavController", function(){
  this.tab = 5;
  this.selectTab = function(setTab) {
    this.tab = setTab;
  };
  this.isSelected = function(checkTab){
    return this.tab === checkTab;
  };
});

app.controller("UsersController", [ '$scope', '$rootScope', '$window', '$http', 'auth', function($scope, $rootScope, $window, $http, auth) {
  var controller = this;
  controller.items = [];
  url = '/api/admin/';
  $rootScope.$on('login:done', function(event) {
    console.log('UsersController received an event' + event.name);
    get();
  });
  function get() {
    $http.get(url + 'users/')
      .success( function(items){
        controller.items = angular.fromJson(items);
      })
      .error( function(data, status, headers, config){
        console.log(status);
      });
  };
  $scope.add = function(item) {
    if (item.role == true) {
      item.role = "Administrator"
    };
    item = angular.toJson(item);
    $http.post(url + 'users/', item)
      .success( function(item){
        item = angular.fromJson(item);
        controller.items.push(item);
      })
      .error( function(data, status, headers, config){
        console.log(status);
      });
  };
  $scope.delete = function(item) {
    $http.delete(url + 'user/' + item.email )
      .success( function(result){
        controller.items.pop(item);
      })
      .error( function(data, status, headers, config){
        console.log(status);
      });
  };
}]);

app.controller("UserApplicationsController", [ '$rootScope', '$scope', '$window', '$http', 'auth', function($rootScope, $scope, $window, $http, auth) {
  var controller = this;
  controller.items = [];
  url = '/api/admin/';
  $rootScope.$on('login:done', function(event) {
    console.log('UserApplicationsController received an event' + event.name);
    get();
  });
  function get() {
    $http.get(url + 'applications/')
      .success( function(items){
        controller.items = angular.fromJson(items);
      })
      .error( function(data, status, headers, config){
        console.log(status);
      });
  };
  $scope.add = function(item) {
    item = angular.toJson(item);
    $http.post(url + 'applications/', item)
      .success( function(item){
        item = angular.fromJson(item);
        controller.items.push(item);
      })
      .error( function(data, status, headers, config){
        console.log(status);
      });
  };
  $scope.delete = function(item) {
    $http.delete(url + 'application/' + item.id )
      .success( function(result){
        controller.items.pop(item);
      })
      .error( function(data, status, headers, config){
        console.log(status);
      });
  };
  $scope.addSubscription = function(item) {
    item = angular.toJson(item);
    $http.post(url + 'subscription/', item)
      .success( function(item){
        item = angular.fromJson(item);
      })
      .error( function(data, status, headers, config){
        console.log(status);
      });
  };
}]);

app.controller("UserSubscriptionsController", [ '$scope', '$rootScope', '$window', '$http', 'auth', function($scope, $rootScope, $window, $http, auth) {
  var controller = this;
  controller.items = [];
  url = '/api/admin/';
  $rootScope.$on('login:done', function(event) {
    console.log('UserSubscriptionsController received an event' + event.name);
    get();
  });
  function get() {
    $http.get(url + 'subscriptions/')
      .success( function(items){
        controller.items = angular.fromJson(items);
      })
      .error( function(data, status, headers, config){
        console.log(status);
      });
  };
  $scope.delete = function(item) {
    $http.delete(url + 'subscription/' + item.UserId + '/' + item.CreationTimeMilli )
      .success( function(result){
        controller.items.pop(item);
      })
      .error( function(data, status, headers, config){
        console.log(status);
      });
  };
}]);

app.controller("UserSessionsController", [ '$scope', '$rootScope', '$window', '$http', 'auth', function($scope, $rootScope, $window, $http, auth) {
  var controller = this;
  controller.items = [];
  url = '/api/admin/';
  $rootScope.$on('login:done', function(event) {
    console.log('UserSessionsController received an event' + event.name);
    get();
  });
  function get() {
    $http.get(url + 'sessions/')
      .success( function(items){
        controller.items = angular.fromJson(items);
      })
      .error( function(data, status, headers, config){
        console.log(status);
      });
  };
}]);

app.controller("ConfigurationController", [ '$scope', '$rootScope', '$window', '$http', 'auth', function($scope, $rootScope, $window, $http, auth) {
  var controller = this;
  controller.item = "";
  url = '/api/admin/';
  $rootScope.$on('login:done', function(event) {
    console.log('ConfigurationController received an event' + event.name);
    get();
  });
  function get() {
    $http.get(url + 'config/')
      .success( function(item){
        $scope.item = item;
        controller.item = angular.fromJson(item);
      })
      .error( function(data, status, headers, config){
        console.log(status);
      });
  };
  $scope.update = function(item) {
    $http.post(url + 'config/', item )
      .success( function(result){
        console.log("Configuration Updated");
      })
      .error( function(data, status, headers, config){
        console.log(status);
      });
  };
}]);