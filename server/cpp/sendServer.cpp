#include <iostream>
#include <fstream>
#include <string>
#include <boost/asio.hpp>

using boost::asio::ip::tcp;

int main() {

    std::ifstream ifs;
    std::string status;
    std::string line;
    ifs.open("./data/dummyStatus.json");
    while (std::getline(ifs, line)) {
        status += line;
    }

    try {
        boost::asio::io_service io_service;
        tcp::acceptor acceptor(io_service, tcp::endpoint(tcp::v4(), 9000));

        while (true) {
            // get connection
            tcp::socket socket(io_service);
            acceptor.accept(socket);

            // write status string
            while (true) {
                boost::system::error_code ignored_error;
                boost::asio::write(socket, boost::asio::buffer("[" + status + "]\n"), ignored_error);
                std::cout << "[" << status << "]" << std::endl;
                // pause 1 second
                sleep(1);
            }

        }

    } catch (std::exception& e) {
        std::cerr << e.what() << std::endl;
    }

    return 0;
}
