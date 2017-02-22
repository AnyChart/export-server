[<img src="https://cdn.anychart.com/images/logo-transparent-segoe.png?2" width="234px" alt="AnyChart - Robust JavaScript/HTML5 Chart library for any project">](https://anychart.com)
# AnyChart Export Server

## Usage 

You can read full description [here](//docs.anychart.com/Common_Settings/Server-side_Rendering).

Export server config file uses [TOML](https://github.com/toml-lang/toml) format:

```
# can be "server" or "cmd"
mode = "server"

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

## License
[© AnyChart.com - JavaScript charts](http://www.anychart.com). Export Server released under the [ECLIPSE PUBLIC LICENSE](https://github.com/AnyChart/export-server/blob/master/LICENSE).