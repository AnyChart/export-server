[<img src="https://cdn.anychart.com/images/logo-transparent-segoe.png?2" width="234px" alt="AnyChart - Robust JavaScript/HTML5 Chart library for any project">](https://anychart.com)
# AnyChart Export Server

## Usage 

You can read full description [here](//docs.anychart.com/Common_Settings/Server-side_Rendering).

Export server can use PhantomJS, Firefox headless or Chrome/Chromium headless.
```
# use -e or --engine flag to specify browser engine
java -jar anychart-export.jar cmd -e firefox --script "var chart = anychart.line([1,2,5]); chart.container('container'); chart.draw();"
```
We recommend use Firefox or PhantomJS as export server engine. 
For now working with Chrome/Chromium is unstable and has been added for future usage.
Default engine is PhantomJS.

### PhantomJS install
* download and install PhantomJS on you PC.
* make sure PhantomJS binary is in your PATH


### Firefox install
* install Firefox browser, version > 56.0
* install `geckodriver`
    * `brew install geckodriver` for Mac users
    * or download it from [official Moziall Size](https://github.com/mozilla/geckodriver/releases) and add it to PATH

### Chrome/Chromium install
* install Chrome or Chromium browser, version > 60.0
* install `chromedriver`
    * `brew install chromedriver` for Mac users
    * or download it from the [official site](https://sites.google.com/a/chromium.org/chromedriver/downloads)  and add it to PATH



### Config file format
Export server config file uses [TOML](https://github.com/toml-lang/toml) format:

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
# settings from twitter app settings for sharint in Twitter
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

To use Twitter sharing be sure you have setup your MySQL database properly with [SQL scheme](https://github.com/AnyChart/export-server/blob/master/src/sql/scheme.sql).

## License
[© AnyChart.com - JavaScript charts](http://www.anychart.com). Export Server released under the [Apache 2.0 License](https://github.com/AnyChart/export-server/blob/master/LICENSE).