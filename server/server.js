var express = require('express');
var fs = require('fs');
var app = express();
var STATUS_FILE_PATH = __dirname+'/data/status.json';

app.get('/', function(req, res) {
  fs.readFile(STATUS_FILE_PATH, function(error, data) {
    res.json(JSON.parse(data));
    console.log('sent');
    fs.writeFileSync(STATUS_FILE_PATH, '[]');
  });
});

var server = app.listen(9000, function() {
  console.log('Listening on port %d', server.address().port);
});
