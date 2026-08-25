#ifndef CUCUMBER_PRETTY_FORMATTER_PRETTY_PRINTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_PRETTY_PRINTER_HPP

#include "cucumber/pretty-formatter/Formatter.hpp"

namespace cucumber::pretty_formatter
{
    struct PrettyPrinter : Formatter
    {
        void Update() override;
    };
}

#endif
