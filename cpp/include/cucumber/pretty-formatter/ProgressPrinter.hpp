#ifndef CUCUMBER_PRETTY_FORMATTER_PROGRESS_PRINTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_PROGRESS_PRINTER_HPP

#include "cucumber/messages/Envelope.hpp"
#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/Formatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <memory>
#include <ostream>

namespace cucumber::pretty_formatter
{
    struct ProgressPrinter : Formatter
    {
        ProgressPrinter(std::ostream& stream, std::shared_ptr<Theme> theme, std::size_t maxWidth);

        void Update(const messages::Envelope& envelope) override;

    private:
        void PrintStatus(messages::TestStepResultStatus status);

        std::ostream& stream;
        std::shared_ptr<Theme> theme;
        std::size_t maxWidth;
        std::size_t width;
    };
}

#endif
