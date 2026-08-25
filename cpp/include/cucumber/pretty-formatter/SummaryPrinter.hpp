#ifndef CUCUMBER_PRETTY_FORMATTER_SUMMARY_PRINTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_SUMMARY_PRINTER_HPP

#include "cucumber/pretty-formatter/Formatter.hpp"

namespace cucumber::pretty_formatter
{
    struct SummaryPrinter : Formatter
    {
        void Update() override;
    };
}

#endif
