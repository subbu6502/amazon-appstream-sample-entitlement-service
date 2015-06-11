app.controller("LoginController", ['$rootScope', '$scope', '$window', 'auth', 'user', 'aws', 'settings', function($rootScope, $scope, $window, auth, user, aws, settings) {
  autoLogin();
  function autoLogin() {
    settings.get().then( function() {
      auth.getAuthorizationProvider().then( function(authorization) {
        user.getCognitoIdentity(authorization).then( function(identity) {
          $scope.identity = identity;
          aws.getAWSCredentials(identity).then( function() {
            $scope.isLoggedIn = true;
            $scope.$apply();
          });
        }, function() {
          $scope.identity = {};
          $scope.isLoggedIn = false;
          $scope.$apply();
        });
      });
    })
  };
  function login(providerName) {
    auth.authorizeProvider(providerName).then( function(authorization) {
      user.getCognitoIdentity(authorization).then( function(identity) {
        $scope.identity = identity;
        aws.getAWSCredentials(identity).then( function() {
          $scope.isLoggedIn = true;
          $scope.$apply();
        });
      }, function() {
        $scope.identity = {};
        $scope.isLoggedIn = false;
        $scope.$apply();
      });
    });
  };
  // user triggered login attempt, interactive as needed
  $scope.doAuth = function(providerName) {
    login(providerName);
  };
  $scope.logout = function() {
    auth.logout();
    $scope.identity = {};
    $scope.isLoggedIn = false;
  };
}]);