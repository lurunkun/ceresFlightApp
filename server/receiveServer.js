var net = require('net');
var fs = require('fs');

var PORT = 3000;

var server = net.createServer();
server.listen(PORT);
console.log('server listening on ', server.address().address +
    ':' + server.address().port);


server.on('connection', function(sock) {
  console.log('CONNECTED: ' + sock.remoteAddress + ':' +
    sock.remotePort);

  sock.on('data', function(data) {
    console.log(data.toString());
  });

});
