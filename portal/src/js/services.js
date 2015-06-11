function AuthorizationProvider($window, settings) {
  var provider = this;

  this.authorize = function(name) {
    return new Promise( function(succeed, fail) {
      settings.get().then( function() {
        switch(name) {
          case 'amazon':
            provider.amazon().then( function(authToken) {
              authorization = {
                name: name,
                token: authToken,
                header: "AmazonOAuth2"
              };
              succeed(authorization); 
            });
            break;
          case 'google':
            provider.google().then( function(authToken) {
              authorization = {
                name: name,
                token: authToken,
                header: "GoogleOAuth2"
              };
              succeed(authorization); 
            });
            break;
          case 'facebook':
            provider.facebook().then( function(authToken) {
              authorization = {
                name: name,
                token: authToken,
                header: "FacebookOAuth2"
              };
              succeed(authorization); 
            });
            break;
        };
      });
    });
  };
  this.amazon = function() {
    return new Promise( function(succeed, fail) {
      options = {};
      options.scope = 'profile';
      options.interactive = 'auto';
      amazon.Login.setClientId(settings.data.loginWithAmazonOAuthClientId);
      login = amazon.Login.authorize(options);
      login.onComplete( function(response) {
        if ( response.error ) {
          error = 'OAuth error: ' + response.error;
          console.log(error);
          fail(response.error);
        } else {
          succeed(response.access_token);
          console.log('Signed in with Amazon');
        }
      });
    });
  };
  this.google = function() {
    return new Promise( function(succeed, fail) {
      gapi = $window.gapi;
      scopes = 'email';
      clientId = settings.data.loginWithGoogleClientId;
      gapi.load('auth', function() {
        gapi.auth.authorize(
          {
            client_id: clientId, 
            scope: scopes, 
            immediate: false}, 
            function(data) {
              access_token = data['access_token'];
              succeed(access_token);
              console.log('Signed in with Google');
            });
      });
    });
  };
  this.facebook = function() {
    return new Promise( function(succeed, fail) {
      FB.init({
        appId      : settings.data.loginWithFacebookOAuthClientId,
        xfbml      : true,
        version    : 'v2.3',
        status     : true,
        cookie     : true
      });
      FB.getLoginStatus(function(response) {
        if (response.status === 'connected') {
          console.log('Signed in with Facebook');
          succeed(response.authResponse.accessToken);
        } else {
          FB.login( function(response) {
            console.log('Signed in with Facebook');
            succeed(response.authResponse.accessToken);
          }, { scope: 'public_profile,email' } );
        }
      });
    })
  };
  this.logout = function() {
    amazon.Login.logout();
    AWS.config.credentials = null;
    FB.logout();
    gapi.auth.signOut();
  };
}

app.factory('auth', ['$window', '$rootScope', '$http', 'settings', function authFactory($window, $rootScope, $http, settings) {
  var auth = this;
  var provider = new AuthorizationProvider($window, settings);
  auth.authorizeProvider = function(providerName) {
    return new Promise( function(succeed, fail) {
      provider.authorize(providerName).then( function(authorization) {
        $http.defaults.headers.common.Authorization = authorization.header + ' ' + authorization.token;
        $window.localStorage.setItem('authorizationProviderName', providerName);
        succeed(authorization);
        $rootScope.$broadcast('login:done');
      });
    });
  };
  auth.getAuthorizationProvider = function() {
    return new Promise( function(succeed, fail) {
      sessionAuthorizationProviderName = $window.localStorage.getItem('authorizationProviderName');
      if (sessionAuthorizationProviderName !== null) {
        provider.authorize(sessionAuthorizationProviderName).then( function(authorization) {
          $http.defaults.headers.common.Authorization = authorization.header + ' ' + authorization.token;
          succeed(authorization);
        });
      };
    });
  };
  auth.logout = function() {
    provider.logout;
  };
  return auth;
}]);

app.factory('user', ['auth', '$http', '$rootScope', '$window', function userFactory(auth, $http, $rootScope, $window) {
  var user = this;
  user.getCognitoIdentity = function(authorization) {
    return new Promise( function(succeed, fail) {
      sessionIdentity = angular.fromJson($window.sessionStorage.getItem('cognitoIdentity'));
      var now = Date.now();
      console.log("Now: " + now);
      if (sessionIdentity != null && now < sessionIdentity.expires ) {
        console.log('Used cached Cognito Identity: ' + (sessionIdentity.id))
        $rootScope.$broadcast('user:done');
        succeed(sessionIdentity);
      } else {
        $http.get('/api/identity/')
          .success( function(identity) {
            console.log('Retrieved Cognito Identity: ' + (identity.id))
            $window.sessionStorage.setItem('cognitoIdentity', angular.toJson(identity));
            $rootScope.$broadcast('user:done');
            succeed(identity);
          } )
          .error( function(data, status, headers, config){
            console.log(status);
          });
      }
    });
  };
  return user;
}]);

app.factory('aws', ['$rootScope', '$window', 'auth', 'user', 'settings', function awsFactory($rootScope, $window, auth, user, settings) {
  var aws = this;
  aws.getAWSCredentials = function(identity) {
    AWS.config.region = settings.data.region;
    return new Promise( function(succeed, fail) {
      sessionAWSCredentials = angular.fromJson($window.sessionStorage.getItem('awsCredentials'));
      console.log('Retrieving new AWS credentials.');
      roleArn = settings.data.roleARN;
      identityPoolId = settings.data.cognitoIdentityPoolId;
      AWS.config.credentials = new AWS.WebIdentityCredentials({
        RoleArn: roleArn,
        WebIdentityToken: identity.token,
        RoleSessionName: 'des'
      });
      AWS.config.credentials.identityId = identity.id;
      succeed();
      $rootScope.$broadcast('login:done');
      $rootScope.$broadcast('aws:done')
    });
  };
  return aws;
}]);

app.service('settings', ['$http', '$window', function settingsFactory($http, $window) {
  var settings = this;
  settings.data = {};
  settings.get = function() {
    return new Promise( function(succeed, fail) { 
      savedSettings = angular.fromJson($window.sessionStorage.getItem('settings'));
      if (savedSettings != null ) {
        angular.merge(settings.data, savedSettings);
        console.log('Using saved settings.');
        succeed();
      } else {
        $http.get('settings.json').then( function(response) {
          var staticSettings = angular.fromJson(response.data);
          angular.merge(settings.data, staticSettings);
          // TODO use authenticated credentials if already available
          settings.getAnonymousCredentials().then ( function() {
            var dynamoDB = DynamoDB(new AWS.DynamoDB({ params: { TableName: staticSettings.configurationDynamoDBTable }, credentials: AWS.config.credentials }));
            var params = {
              Select: 'SPECIFIC_ATTRIBUTES',
              ProjectionExpression: 'loginWithAmazonOAuthClientId, loginWithFacebookOAuthClientId, loginWithGoogleClientId',
              KeyConditions: [
                dynamoDB.Condition('StackId', 'EQ', staticSettings.stackName)
              ]
            };
            dynamoDB.query(params, function(err, data) {
              if (err) console.log(err, err.stack);
              else {
                var remoteSettings = angular.fromJson(data.Items[0]);
                angular.merge(settings.data, remoteSettings);
                $window.sessionStorage.setItem('settings', angular.toJson(settings.data));
                succeed();
              }
            });
          });
        });
      };
    });
  };
  settings.getAnonymousCredentials = function() {
    return new Promise( function(succeed, fail) {
      AWS.config.region = settings.data.region;
      AWS.config.credentials = new AWS.CognitoIdentityCredentials({
        IdentityPoolId: settings.data.cognitoIdentityPoolId
      });
      succeed();
    });
  };
  return settings;
}]);