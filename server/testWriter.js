var fs = require('fs');
var lockFile = require('lockfile');

fs.truncateSync(__dirname+'/data/status.json', 0);

// create status.json array if empty
var buff = fs.readFileSync(__dirname+'/data/status.json')
if (buff.length === 0) {
  fs.writeFileSync(__dirname+'/data/status.json', '[]');
}

setInterval(function() {
  lockFile.lockSync('stats.lock');
  writeStats('dummyStatus');
  lockFile.unlockSync('stats.lock');
}, 10);

setInterval(function() {
  lockFile.lockSync('stats.lock');
  writeStats('dummyError');
  lockFile.unlockSync('stats.lock');
}, 10);


function writeStats(type) {
  var status = fs.readFileSync(__dirname+'/data/status.json');
  var data = fs.readFileSync(__dirname + '/data/' + type + '.json');
  status = JSON.parse(status);
  status.push(JSON.parse(data));
  fs.writeFileSync(__dirname+'/data/status.json', JSON.stringify(status));
  console.log('write dummy ' + type);
}

