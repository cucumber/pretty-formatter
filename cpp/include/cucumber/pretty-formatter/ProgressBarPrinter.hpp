#ifndef CUCUMBER_PRETTY_FORMATTER_PROGRESS_BAR_PRINTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_PROGRESS_BAR_PRINTER_HPP

#include "cucumber/messages/Envelope.hpp"
#include "cucumber/pretty-formatter/Formatter.hpp"
#include <ostream>

namespace cucumber::pretty_formatter
{
    struct ProgressBarPrinter : Formatter
    {
        ProgressBarPrinter(std::ostream& stream);

        void Update(const messages::Envelope& envelope) override;
    };
}

#endif
