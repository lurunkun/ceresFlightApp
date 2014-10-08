#include <iostream>
#include <string>
#include <boost/asio.hpp>

using boost::asio::ip::tcp;

int main() {
    try {
        boost::asio::io_service io_service;
        tcp::acceptor acceptor(io_service, tcp::endpoint(tcp::v4(), 3000));

        while (true) {
            // get connection
            tcp::socket socket(io_service);
            acceptor.accept(socket);
            boost::system::error_code ignored_error;

            // read stream
            boost::asio::streambuf b;
            boost::asio::read_until(socket, b, "\n");

            // print string
            std::istream is(&b);
            std::string line;
            std::getline(is, line);

            std::cout << line << std::endl;
        }

    } catch (std::exception& e) {
        std::cerr << e.what() << std::endl;
    }

    return 0;
}
