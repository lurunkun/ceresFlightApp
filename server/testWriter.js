var fs = require('fs');

// create status.json array if empty
var buff = fs.readFileSync(__dirname+'/data/status.json')
if (buff.length === 0) {
  fs.writeFileSync(__dirname+'/data/status.json', '[]');
}

setInterval(function() {
  fs.readFile(__dirname+'/data/status.json', function(err, status) {
    var status = JSON.parse(status);
    fs.readFile(__dirname+'/data/dummyStatus.json', function(readErr, dummyStatus) {
      if (readErr) console.log(readErr);
      status.push(JSON.parse(dummyStatus));
      fs.writeFile(__dirname+'/data/status.json', JSON.stringify(status), function(writeErr) {
        if (writeErr) console.log(writeErr);
        console.log('write dummy status');
      });
    });
  });
}, 500);

setInterval(function() {
  fs.readFile(__dirname+'/data/status.json', function(err, status) {
    var status = JSON.parse(status);
    fs.readFile(__dirname+'/data/dummyError.json', function(readErr, dummyError) {
      if (readErr) console.log(readErr);
      status.push(JSON.parse(dummyError));
      fs.writeFile(__dirname+'/data/status.json', JSON.stringify(status), function(writeErr) {
        if (writeErr) console.log(writeErr);
        console.log('write dummy error');
      });
    });
  });
}, 5000);
