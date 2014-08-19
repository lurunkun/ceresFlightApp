var fs = require('fs');
var lockFile = require('lockfile');

fs.truncateSync(__dirname+'/data/status.json', 0);

// create status.json array if empty
var buff = fs.readFileSync(__dirname+'/data/status.json')
if (buff.length === 0) {
  fs.writeFileSync(__dirname+'/data/status.json', '[]');
}

setInterval(function() {
      lockFile.lock('status.lock', function() {
        writeStats('dummyStatus');
      });
}, 5);

setInterval(function() {
      lockFile.lock('status.lock', function() {
        writeStats('dummyError');
      });
}, 20);


function writeStats(type) {
  fs.readFile(__dirname+'/data/status.json', function(err, status) {
    var status = JSON.parse(status);
    fs.readFile(__dirname+'/data/' + type + '.json', function(readErr, dummyData) {
      if (readErr) console.log(readErr);
      status.push(JSON.parse(dummyData));
      fs.writeFile(__dirname+'/data/status.json', JSON.stringify(status), function(writeErr) {
        if (writeErr) console.log(writeErr);
        console.log('write dummy ' + type);
      });
    });
  });
}

