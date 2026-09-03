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
        struct Tty
        {
            Tty() = default;
            virtual ~Tty() = default;

            Tty(const Tty&) = delete;
            Tty(Tty&&) = delete;

            Tty& operator=(const Tty&) = delete;
            Tty& operator=(Tty&&) = delete;

            virtual void Write(std::string_view output) = 0;
            virtual void ClearScreenDown() = 0;
            virtual void MoveCursorUp(std::size_t lines) = 0;
        };

        struct TtyOstream : Tty
        {
            TtyOstream(std::ostream& stream);
            ~TtyOstream() override = default;

            TtyOstream(const TtyOstream&) = delete;
            TtyOstream& operator=(const TtyOstream&) = delete;

            TtyOstream(TtyOstream&&) = delete;
            TtyOstream& operator=(TtyOstream&&) = delete;

            void Write(std::string_view output) override;
            void ClearScreenDown() override;
            void MoveCursorUp(std::size_t lines) override;

        private:
            std::ostream& stream;
        };

        ProgressBarPrinter(Tty& tty, std::shared_ptr<Theme> theme, std::size_t maxWidth);
        ~ProgressBarPrinter() override;

        ProgressBarPrinter(const ProgressBarPrinter&) = delete;
        ProgressBarPrinter& operator=(const ProgressBarPrinter&) = delete;

        ProgressBarPrinter(ProgressBarPrinter&&) = delete;
        ProgressBarPrinter& operator=(ProgressBarPrinter&&) = delete;

        void Update(const messages::Envelope& envelope) override;

    private:
        void Rerender();

        struct Data;
        std::unique_ptr<Data> data;

        struct Printer;
        std::unique_ptr<Printer> printer;
    };
}

#endif
