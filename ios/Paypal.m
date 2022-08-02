#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(Paypal, NSObject)

RCT_EXTERN_METHOD(
  requestBillingAgreement: (NSDictionary*)options
  resolver: (RCTPromiseResolveBlock)resolve
  rejecter: (RCTPromiseRejectBlock)reject
)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
