var fs = require('fs');
var lockFile = require('lockfile');

fs.truncateSync(__dirname+'/data/status.json', 0);

// create status.json array if empty
var buff = fs.readFileSync(__dirname+'/data/status.json')
if (buff.length === 0) {
  fs.writeFileSync(__dirname+'/data/status.json', '[]');
}

setInterval(function() {
      lockFile.lock('status.lock',{retries: 10, retryWait: 1}, function(err) {
        if (err) console.log(err);
        writeStats('dummyStatus', function() {
          lockFile.unlockSync('status.lock');
        });
      });
}, 50);

setInterval(function() {
      lockFile.lock('status.lock',{retries: 10, retryWait: 1}, function(err) {
        if (err) console.log(err);
        writeStats('dummyError', function() {
          lockFile.unlockSync('status.lock');
        });
      });
}, 200);


function writeStats(type, callback) {
  fs.readFile(__dirname+'/data/status.json', function(err, status) {
    var status = JSON.parse(status);
    fs.readFile(__dirname+'/data/' + type + '.json', function(readErr, dummyData) {
      if (readErr) console.log(readErr);
      status.push(JSON.parse(dummyData));
      fs.writeFile(__dirname+'/data/status.json', JSON.stringify(status), function(writeErr) {
        if (writeErr) console.log(writeErr);
        console.log('write dummy ' + type);
        callback();
      });
    });
  });
}

