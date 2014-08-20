var express = require('express');
var fs = require('fs');
var lockFile = require('lockfile');
var app = express();
var STATUS_FILE_PATH = __dirname+'/data/status.json';
var opt = {  stale : 20, pollPeriod : 1, retries : 10 };

app.get('/', function(req, res) {
  lockFile.lockSync('stats.lock', opt);
  var data = fs.readFileSync(STATUS_FILE_PATH);
  res.json(JSON.parse(data));
  console.log('sent');
  fs.writeFileSync(STATUS_FILE_PATH, '[]');
  lockFile.unlockSync('stats.lock');
});

var server = app.listen(9000, function() {
  console.log('Listening on port %d', server.address().port);
});
