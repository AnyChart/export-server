[<img src="https://cdn.anychart.com/images/logo-transparent-segoe.png?2" width="234px" alt="AnyChart - Robust JavaScript/HTML5 Chart library for any project">](https://anychart.com)
# AnyChart Export Server

AnyChart Export Server is a tool that is used to provide chart export to PNG, JPG, PDF, SVG, CSV, Excel, JSON and XML.
You can read the full description [here](//docs.anychart.com/Common_Settings/Server-side_Rendering).

## Setup 
Export server can use PhantomJS, Firefox headless or Chrome/Chromium headless.
```
# use -e or --engine flag to specify browser engine
java -jar anychart-export.jar cmd -e firefox --script "var chart = anychart.line([1,2,5]); chart.container('container'); chart.draw();"
```
Default browser engine is PhantomJS.

If you need to take a screenshot of a page on the Internet or in a local HTML file, you can just use
Chrome or Firefox in headless mode without the AnyChart Export Server. You can read about that
[here](https://developers.google.com/web/updates/2017/04/headless-chrome), for Chrome and
[here](https://developer.mozilla.org/en-US/Firefox/Headless_mode), for Firefox.
Don't forget to use `file:///` prefix for rendering a local HTML file.

### PhantomJS install
* download and install PhantomJS on you PC.
* make sure PhantomJS binary is in your PATH

### Firefox install
* install Firefox browser (version 56.0 and above)
* install `geckodriver`
    * `brew install geckodriver` for Mac users
    * or download it from [official Mozilla Site](https://github.com/mozilla/geckodriver/releases) and add it to PATH

### Chrome/Chromium install
* install Chrome or Chromium browser (version 60.0 and above)
* install `chromedriver`
    * `brew install chromedriver` for Mac users
    * or download it from the [official site](https://sites.google.com/a/chromium.org/chromedriver/downloads)  and add it to PATH


## Requests
AnyChart Export Server supports the following requests:

| Request       | Type          | Description  |
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
| /sharing/twitter_oauth | GET     |    Twitter Sharing auth request |
| /sharing/twitter_confirm | POST     |    Twitter Sharing status update |


Typical Export and Twitter Sharing request contains the params listed below:

| Parameter      | Type         | Default| Description  |
| ------------- |:-------------:|--|------|
| data        | **required**   | - |script or svg that should be transformed into a picture |
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
AnyChart Export Server provides an ability to pass all parameters within a config file using [TOML](https://github.com/toml-lang/toml) format:

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

You can pass a config file witn `-C` option, e.g.

```
java -jar export-server.jar -C settings.toml
 ```

## Sharing
The AnyChart Export Server provides an ability to share chart images in social networks, such as Facebook,
LinkedIn, Pinterest and Twitter.
When you use the Export Server on your own server and you want the sharing properly working, you should
set up `--saving-folder` and `--saving-prefix`. The first parameter is the path to the folder where images will be
stores. The second parameter is the URL prefix which will be concatenated with saved image name and returned to user.
You should provide the access to saved image through that URL by e.g. setuping Nginx.


### Sharing on Facebook, LinkedIn and Pinterest
Sharing on Facebook, LinkedIn, and Pinterest is implemented with the help of the commands for saving images.
Mentioned social networks get the prepared picture via the link and just allow a user to post it on the page.

### Sharing on Twitter
Sharing images on Twitter is implemented with the AnyChart Twitter app, needs MySQL databases to be setup
and uses three types of requests.

1. First of all, user sends a request to `/sharing/twitter` that contains svg or script which image will be posted on
the page. There are two options here: a user is authenticated in the AnyChart Twitter app or not.

If the user isn't authenticated, the Twitter Authorization dialog will be displayed. The user should confirm that he
gives the app the rights fot posting image. After that there will be redirect to the collback `/sharing/twitter_oauth`.

2. In the handler of `/sharing/twitter_oauth` request. The Export Server gets such params as oauth_token,
oauth_token_secret, user_id, screen_name, user_pic and user_name and saves them to MySQL database.
After that the posting dialog will be displayed.

If a user is already authenticated in the app, the posting dialog will be displayed immidiately. When the user confirms
to post the image and click the TWEET button, there will be a request to `/sharing/twitter_confirm `.

3, In the handler of `/sharing/twitter_confirm` request, the Export Server upload the shared image with Twitte API and
posts new tweet with that image.

To setup the MySQL database for Twitter sharing use [SQL scheme](https://github.com/AnyChart/export-server/blob/master/src/sql/scheme.sql).

## License
[© AnyChart.com - JavaScript charts](http://www.anychart.com). Export Server released under the [Apache 2.0 License](https://github.com/AnyChart/export-server/blob/master/LICENSE).