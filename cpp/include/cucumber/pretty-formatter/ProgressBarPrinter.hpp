#ifndef CUCUMBER_PRETTY_FORMATTER_PROGRESS_BAR_PRINTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_PROGRESS_BAR_PRINTER_HPP

#include "cucumber/pretty-formatter/Formatter.hpp"

namespace cucumber::pretty_formatter
{
    struct ProgressBarPrinter : Formatter
    {
        void Update() override;
    };
}

#endif
