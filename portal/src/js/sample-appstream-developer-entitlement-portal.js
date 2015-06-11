angular.module('ui.bootstrap.buttons', [])
  .directive('btnWatch', ['$interval', function($interval) {
    function link(scope, element, attrs) {
      var loadStartTimeMilli;
      scope.$watch(attrs.btnWatch, function(value) {
        if (typeof value === "undefined") {
          // console.log('session undefined');
          element.data('resetText', element.html());
        } else if (value === 'loading') {
          // console.log('session loading');
          loadStartTimeMilli = (new Date).getTime();
          element.addClass("disabled").attr("disabled","disabled");
          element.children("a").text(attrs.loadingText);
        } else if (value === 'expired') {
          // console.log('session expired');
        } else {  // load complete
          // console.log('session loaded');
          element.removeClass("disabled").removeAttr("disabled")
          element.children("a").text(attrs.completeText);
          timeout = $interval(function() {
            if ((new Date).getTime() > (loadStartTimeMilli + 60000)) {
              element.removeClass("disabled").removeAttr("disabled")
              // console.log(element);
              element.children("a").text('Start Again');
              // console.log('reset time reached');
              $interval.cancel(timeout);
            }
          }, 1000);
          element.on('$destroy', function() {
            $interval.cancel(timeout);
          });
        }
      });
    };
    return {
      link: link
    };
  }]);

var app = angular.module('portal', ['ui.bootstrap']);

app.controller("NavController", function(){
  this.tab = 1;
  this.selectTab = function(setTab) {
    this.tab = setTab;
  };
  this.isSelected = function(checkTab){
    return this.tab === checkTab;
  };
});

app.controller("SubscriptionsController", [ '$rootScope', '$scope', '$window', '$http', 'auth', 'aws', 'settings', function($rootScope, $scope, $window, $http, auth, aws, settings) {
  var controller = this;
  controller.items = [];
  $rootScope.$on('aws:done', function(event) {
    console.log('SubscriptionsController received an event ' + event.name);
    get();
  });
  function get(){
    console.log(settings.data.userSubscriptionDynamoDBTable);
    var dynamoDB = DynamoDB(new AWS.DynamoDB({ params: { TableName: settings.data.userSubscriptionDynamoDBTable }, credentials: AWS.config.credentials }));
    var identityId = AWS.config.credentials.identityId;
    var params = {
      Select: 'SPECIFIC_ATTRIBUTES',
      ProjectionExpression: 'Email, UserId, CreationTimeMilli, UserApplicationId, AppStreamApplicationId, UserApplicationDescription, UserApplicationName, PerSessionTimeLimitMilli, TotalCombinedSessionTimeLimitMilli',
      KeyConditions: [
        dynamoDB.Condition('UserId', 'EQ', identityId)
      ]
    }
    dynamoDB.query(params, function(err, data) {
      if (err) console.log(err, err.stack);
      else {
          controller.items = data.Items;
          $scope.$apply();
      }
    });
  };
  this.sessionBtn = function(index) {
    url = '/api/entitlements/';
    webRTCURLBase = 'http://client.appstream.amazonaws.com/?entitlementUrl=';
    var subscription = controller.items[index];
    var session = subscription.session;
    if (typeof session === "undefined") {
      controller.items[index].session = 'loading';
      $http.post(url + 'session/startsession/', controller.items[index])
        .success( function(response){
          controller.items[index].session = angular.fromJson(response);
        })
        .error( function(data, status, headers, config){
          console.log(status);
        });
    } else if ( session.AppStreamEntitlementUrl ) {
      $window.open(webRTCURLBase + session.AppStreamEntitlementUrl, '_blank');
    };
  };
}]);