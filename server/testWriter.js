var fs = require('fs');
var lockFile = require('lockfile');

fs.truncateSync(__dirname+'/data/status.json', 0);

// create status.json array if empty
var buff = fs.readFileSync(__dirname+'/data/status.json')
if (buff.length === 0) {
  fs.writeFileSync(__dirname+'/data/status.json', '[]');
}

var opt = {  stale : 50, pollPeriod : 2, retries : 100 };

var count = 0;
setInterval(function() {
  if (count > 5) count = 0;

  // write status
  try {
    lockFile.lockSync('stats.lock', opt);
    writeStats('dummyStatus');
    // write error
    if (count === 5) {
      writeStats('dummyError');
    }
    lockFile.unlockSync('stats.lock');
    count++;
  } catch (e) {
    console.log(e);
  }
}, 50);

function writeStats(type) {
  var status = fs.readFileSync(__dirname+'/data/status.json');
  var data = fs.readFileSync(__dirname + '/data/' + type + '.json');
  status = JSON.parse(status);
  status.push(JSON.parse(data));
  fs.writeFileSync(__dirname+'/data/status.json', JSON.stringify(status));
  console.log('write dummy ' + type);
}


// module.exports = function() {
//   var count = 0;
//   setInterval(function() {
//     if (count > 5) count = 0;
//
//     // write status
//     try {
//       lockFile.lockSync('stats.lock', opt);
//       writeStats('dummyStatus');
//       // write error
//       if (count === 5) {
//         writeStats('dummyError');
//       }
//       lockFile.unlockSync('stats.lock');
//       count++;
//     } catch (e) {
//       console.log(e);
//     }
//   }, 10);
// }
//
//
