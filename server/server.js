// var express = require('express');
var net = require('net');
var fs = require('fs');
// var lockFile = require('lockfile');
// var sleep = require('sleep');
// var app = express();
// var STATUS_FILE_PATH = __dirname+'/data/status.json';
// var opt = {  stale : 10, pollPeriod : 10, retries : 0 };
// var restarted = false;
// var testWriter = require('./testWriter.js');
var PORT = 9000;

var server = net.createServer();
server.listen(PORT);
console.log('server listening on ', server.address().address +
    ':' + server.address().port);

server.on('connection', function(sock) {
  console.log('CONNECTED: ' + sock.remoteAddress + ':' +
    sock.remotePort);

  writeStatus(sock);

});

function writeStatus(sock) {
  var count = 0;
  setInterval(function() {
    if (count > 20) count = 0;
    // write status
    try {
      writeStats('dummyStatus', sock);
      // write error
      if (count === 20) {
        writeStats('dummyError', sock);
      }
      if (count === 10 || 15) {
        writeStats('dummyWarning', sock);
      }
      count++;
    } catch (e) {
      console.log(e);
    }
  }, 1000);
}

function writeStats(type, sock) {
  var status = fs.readFileSync(__dirname+'/data/status.json');
  var data = fs.readFileSync(__dirname + '/data/' + type + '.json');
  status = JSON.parse(status);
  data = JSON.parse(data);
  data.timeStamp = new Date().toTimeString() + " ms: " + new Date().getMilliseconds();
  console.log(data);
  status.push(data);
  sock.write(JSON.stringify(status));
  console.log('write dummy ' + type);
}

// app.get('/', function(req, res) {
//   try {
//     lockFile.lockSync('stats.lock', opt);
//     var data = fs.readFileSync(STATUS_FILE_PATH);
//     console.log('sent');
//     fs.writeFileSync(STATUS_FILE_PATH, '[]');
//     lockFile.unlockSync('stats.lock');
//     data = JSON.parse(data);
//     if (restarted === true) {
//       data.status = 'Restarted';
//       restarted = false;
//     }
//     res.json(data);
//   } catch (e) {
//     console.log(e);
//     res.json([]);
//   } finally {
//     return;
//   }
// });

// app.get('/reboot', function(req, res) {
//   console.log("restarting");
//   res.send("restarting");
//   sleep.sleep(10);
//   try {
//     lockFile.lockSync('status.lock', opt);
//     fs.writeFileSync(STATUS_FILE_PATH, '[]');
//     lockFile.unlockSync('status.lock');
//   } catch(e) {
//     console.log(e);
//   } finally {
//     restarted = true;
//     return;
//   }
// });
//
// // testWriter();
//
// var server = app.listen(9000, function() {
//   console.log('Listening on port %d', server.address().port);
// });
