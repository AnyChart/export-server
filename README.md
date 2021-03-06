[<img src="https://cdn.anychart.com/images/logo-transparent-segoe.png?2" width="234px" alt="AnyChart - Robust JavaScript/HTML5 Chart library for any project">](https://anychart.com)
# AnyChart Export Server

AnyChart Export Server is a tool that is helps to export charts to PNG, JPG, PDF, SVG, CSV, Excel, JSON, and XML.
You can read the full description in [AnyChart Export Server Documentation](//docs.anychart.com/Common_Settings/Server-side_Rendering).

## Setup 
Export server can use PhantomJS, headless Firefox or headless Chrome/Chromium.
```
# use -e or --engine flag to specify browser engine
java -jar anychart-export.jar cmd -e firefox --script "var chart = anychart.line([1,2,5]); chart.container('container'); chart.draw();"
```
Default browser engine is PhantomJS.

If you need to take a screenshot of a page on the Internet or of a local HTML file, you can just use
Chrome or Firefox in headless mode without AnyChart Export Server. You can read about that
[headless Chrome documentation](https://developers.google.com/web/updates/2017/04/headless-chrome), for Chrome and in
[headless Firefox documentation](https://developer.mozilla.org/en-US/Firefox/Headless_mode), for Firefox.
Don't forget to use `file:///` prefix for rendering local HTML files.

### PhantomJS install
Download and install PhantomJS to you PC from the [PhantomJS official site](http://phantomjs.org/download.html).

Add PhantomJS binary to the [PATH](https://stackoverflow.com/a/14638025).

Check if it works properly:
```
$ phantomjs -v
2.1.1
```

### Firefox install
Install Firefox browser (version 56.0 and above).

For Debian-based Linux distros:
```
$ sudo apt-get update && sudo apt-get install firefox -y
```
To check Firefox version:
```
$ firefox -v
Mozilla Firefox 60.0.1
```
To check if the browser works properly:
```
$ firefox -headless -screenshot https://developer.mozilla.com
```
A screenshot of Mozilla site must appear in your working directory.

Install `geckodriver`.
For Mac, use:
```
$ brew install geckodriver
``` 
If you use Linux or Windows, you can download it from the [geckodriver official site](https://github.com/mozilla/geckodriver/releases) 

Add it to the [PATH](https://stackoverflow.com/a/14638025).

To check if it is installed properly, use the next command:
```
$ geckodriver --version
geckodriver 0.19.1
```

Now you are ready to lauch the AnyChart Export Server.

### Chrome/Chromium install
Install Chrome or Chromium browser (version 60.0 and above).

For Debian-based Linux distros we suggest to install Chromium:
```
$ sudo apt-get update && sudo apt-get install chromium-browser -y
``` 
To check Chromium version, type:
```
$ chromium-browser --version
Chromium 66.0.3359.181 Built on Ubuntu , running on Ubuntu 16.04
```
To check the browser works properly, use:
```
$ chromium-browser --headless --no-sandbox --disable-gpu http://google.com   
```
A screenshot of the Google site must appear in your working directory.

Install `chromedriver`.
For Mac, use:
```
$ brew install chromedriver
``` 
If you use Linux or Windows, download it from the [chromedriver official site](https://sites.google.com/a/chromium.org/chromedriver/downloads).

Add the binary to the [PATH](https://stackoverflow.com/a/14638025).

To check if it is installed properly, use:
```
$ chromedriver -v
ChromeDriver 2.38.552522 (437e6fbedfa8762dec75e2c5b3ddb86763dc9dcb)
```

Now you are ready to launch the AnyChart Export Server.

## Requests
AnyChart Export Server supports the following requests:

| URL       | Type          | Description  |
| ------------- |:-------------:|------|
| /status       | GET or POST   | Server status |
| /png          | POST      |   Export to PNG |
| /jpg          | POST       |   Export to JPG |
| /svg          | POST      |    Export to SVG |
| /pdf | POST      |    Export to PDF|
| /xml | POST     |    Export to XML |
| /json |POST     |    Export to JSON |
| /csv |POST     |    Export to CSV |
| /xlsx | POST     |    Export to XLSX |
| /sharing/twitter | POST     |  Twitter Sharing request   |
| /sharing/twitter_oauth | GET     |    Twitter Sharing OAuth callback |
| /sharing/twitter_confirm | POST     |    Twitter Sharing posting confirmation |


Typical Export and Twitter Sharing request contains the parameters listed below:

| Parameter      | Type         | Default| Description  |
| ------------- |:-------------:|--|------|
| data        | **required**   | - |script or SVG that should be transformed into a picture |
| data-type   | **required**   | - | a field that contains the information about the data, it might be "script" or "svg"|
| response-type | **required**  | - | a field that tells how to export the result (file or as base64) |
| file-name    | optional | anychart |    file name |
| save       | optional   | - |if it presents, request returns URL of a saved image|
| container-id     | optional |container| div container id|
| container-width  | optional |100%| div container width|
| container-height | optional |100%| div container height|
| width | optional      | 1024|   image width|
| height | optional      | 800|   image height|
| quality | optional      |  1|   picture quality|
| force-transparent-white | optional   | false | make the background white or leave it transparent|
| pdf-size | optional | - | the *.pdf-document sheet size|
| pdf-x | optional | 0| x of the chart in the *.pdf document|
| pdf-y | optional | 0| y of the chart in the *.pdf document|
| pdf-width | optional | 595| pdf width|
| pdf-height | optional |842| pdf height|
| landscape | optional | false |the document orientation|

## Config file format
AnyChart Export Server provides an ability to pass all parameters in a config file using [TOML](https://github.com/toml-lang/toml) format:

```
# can be "server" or "cmd"
mode = "server"
engine = "firefox"

[server]
port = 80
host = "0.0.0.0"
allow-scripts-executing = false
log = "/path/to/log/file"

[server.images]
# folder to save images
folder = "/export-server/shared"
# prefix which will be returned when saving image
prefix = "http://static.example.com/shared/"

[server.sharing]
# MySQL settings
port = 3306
db = "shared_db"
user = "export_server_user"
password = "export_server_password"

[server.sharing.twitter]
# settings from twitter app settings for sharing in Twitter
key = "key"
secret = "secret"
callback = "http://example.com/sharing/twitter_oauth"

[cmd] 
# here you can pass cmd options for mode = "cmd"
script = "var chart = anychart.line([1,2,5]); chart.container('container'); chart.draw();" 
output-file = "anychart"
output-path = ""
container-width = "1024px"
container-height = "800px"
container-id = "container"
input-file = "file.name"
type = "png"
image-width = 1024
image-height = 800
force-transparent-white = false
pdf-size = "a4"
pdf-x = 0
pdf-y = 0
pdf-width  = 500
pdf-height = 500
jpg-quality = 1
```

You can pass a config file with `-C` option, like this:

```
java -jar export-server.jar -C settings.toml
 ```

## Sharing
The AnyChart Export Server provides an ability to share chart images in social networks, such as Facebook,
LinkedIn, Pinterest and Twitter.
When you use the Export Server on your own server and you want the sharing to work properly, you should set up `--saving-folder` and `--saving-prefix`. The first parameter is the path to the folder where images will be stored. The second parameter is the URL prefix which will be concatenated with a shared image name and returned to a user. You should provide the access to shared image through that URL by setting up Nginx, for example.

### Sharing on Facebook, LinkedIn, and Pinterest
Sharing on Facebook, LinkedIn, and Pinterest is implemented with the help of the commands for saving images. These social networks get the prepared picture via the link and just allow the user to post it on the page.

### Sharing on Twitter
Sharing images on Twitter is implemented with the AnyChart Twitter app. It requires MySQL database to be set up and uses three types of requests.

#### `/sharing/twitter` 
First of all, the user sends a request to `/sharing/twitter` that contains SVG or script  from which the image will be generated and posted on the page - the request should contain the same parameters as a request to `/png` URL does.

There are two options here: the user is authorized in the AnyChart Twitter application or not.

If the user isn't authorized, the Twitter Authorization dialog will be displayed. The user should confirm that he gives the app the rights to post the image. After that, the user will be redirected to the `/sharing/twitter_oauth` callback.

#### `/sharing/twitter_oauth`
This request accepts `oauth_token` and `oauth_verifier` parameters, you can read about [OAuth here](https://en.wikipedia.org/wiki/OAuth).
In the handler of /sharing/twitter_oauth request, the Export Server gets such params as oauth_token, oauth_token_secret, user_id, screen_name, image_url (user picture) and user_name and saves them to the MySQL database. After that, the dialog window of posting images will be displayed.

If the user is already authorized in the app, the posting dialog will be displayed immediately. When the user confirms to post the image and clicks the TWEET button, there will be a request to `/sharing/twitter_confirm `.

#### `/sharing/twitter_confirm`
This request should contain Twitter `message` parameter only - a string of no more than 140 characters. In the handler of `/sharing/twitter_confirm` request, the Export Server uploads the shared image with Twitter API and posts a new tweet with that image.

Note: the `/sharing/twitter_oauth` and `/sharing/twitter_confirm` requests are used inside Export server, which means you don't need to send anything by yourself there.

If you want Twitter sharing to work through your server, you should:
1. create your own Twitter App and provide `twitter_key`, `twitter_secret` and `twitter_callback` 
(last path of which is always `/sharing/twitter_oauth`) to the Export Server. 
2. setup a MySQL database for Twitter sharing using [SQL scheme](https://github.com/AnyChart/export-server/blob/master/src/sql/scheme.sql).
3. setup a Twitter sharing URL separately when setting the `anychart.export.server()` URL:
```javascript
anychart.exports.twitter(
    "http://your.export.server.url/sharing/twitter", 
    "1000",    
    "500"
);
```
For more details check out [AnyChart Sharing API](http://api.anychart.com/anychart.exports).

## License
[© AnyChart.com - JavaScript charts](http://www.anychart.com). Export Server released under the [Apache 2.0 License](https://github.com/AnyChart/export-server/blob/master/LICENSE).
