var express = require('express');
var fs = require('fs');
var lockFile = require('lockfile');
var app = express();
var STATUS_FILE_PATH = __dirname+'/data/status.json';
var opt = {  stale : 10, pollPeriod : 10, retries : 0 };
// var testWriter = require('./testWriter.js');


app.get('/', function(req, res) {
  try {
    lockFile.lockSync('stats.lock', opt);
    var data = fs.readFileSync(STATUS_FILE_PATH);
    console.log('sent');
    fs.writeFileSync(STATUS_FILE_PATH, '[]');
    lockFile.unlockSync('stats.lock');
    res.json(JSON.parse(data));
  } catch (e) {
    console.log(e);
    res.send('error');
  } finally {
    return;
  }
});

// testWriter();

var server = app.listen(9000, function() {
  console.log('Listening on port %d', server.address().port);
});
