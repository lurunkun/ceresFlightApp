var express = require('express');
var fs = require('fs');
var lockFile = require('lockfile');
var app = express();
var STATUS_FILE_PATH = __dirname+'/data/status.json';

app.get('/', function(req, res) {
  fs.readFile(STATUS_FILE_PATH, function(error, data) {
    res.json(JSON.parse(data));
    console.log('sent');
    lockFile.lockSync('stats.lock');
    fs.writeFileSync(STATUS_FILE_PATH, '[]');
    lockFile.unlockSync('stats.lock');
  });
});

var server = app.listen(9000, function() {
  console.log('Listening on port %d', server.address().port);
});
