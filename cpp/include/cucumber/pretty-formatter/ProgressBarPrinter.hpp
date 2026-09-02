#ifndef CUCUMBER_PRETTY_FORMATTER_PROGRESS_BAR_PRINTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_PROGRESS_BAR_PRINTER_HPP

#include "cucumber/messages/Envelope.hpp"
#include "cucumber/pretty-formatter/Formatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <memory>
#include <ostream>
#include <string_view>

namespace cucumber::pretty_formatter
{
    struct ProgressBarPrinter : Formatter
    {
        ProgressBarPrinter(std::ostream& stream, std::unique_ptr<Theme> theme, std::size_t maxWidth,
            std::string_view clearLine = "\x1b[{}A\x1b[0J");
        ~ProgressBarPrinter() override;

        ProgressBarPrinter(const ProgressBarPrinter&) = delete;
        ProgressBarPrinter& operator=(const ProgressBarPrinter&) = delete;

        ProgressBarPrinter(ProgressBarPrinter&&) = delete;
        ProgressBarPrinter& operator=(ProgressBarPrinter&&) = delete;

        void Update(const messages::Envelope& envelope) override;

    private:
        void Rerender();

        std::unique_ptr<Theme> theme;

        struct Data;
        std::unique_ptr<Data> data;

        struct Printer;
        std::unique_ptr<Printer> printer;
    };
}

#endif
