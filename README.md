# Serverless project to create report, get report document from SP-API

## Set up instructions
* Install serverless https://www.serverless.com/
* change the profile value in serverless.yml to aws profile you set on your cli.
* Set up environment configuration for SP-API in serverless.yml file. Add following configuration under provider > environment key
     * refreshToken: "SP-API App refresh token"
     * clientId: "SP-API App client id"
     * clientSecret: "SP-API App client secret"
     * oauthUrl: "OAuth url"
     * spApiAwsAccountAccessKey: "Access key associated with role registered with SP-API"
     * spApiAwsAccountSecretKey: "Secret key associated with role registered with SP-API"
     * spApiServiceName: "execute-api"
     * spApiRegion: ""
     * spApiBaseUrl: "https://sellingpartnerapi-na.amazon.com/reports/2020-09-04/"
     * serverlessAppAccessKey: "Access key to deploy to app in to your aws account"
     * serverlessAppSecretKey: "Secret key to deploy to app in to your aws account"
     * destinationS3Bucket: "Bucket where to write sp-api document data"
* From root of project execute following commands 
  * mvn clean install
  * serverless deploy


