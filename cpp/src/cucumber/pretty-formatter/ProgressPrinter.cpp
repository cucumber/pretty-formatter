#include "cucumber/pretty-formatter/ProgressPrinter.hpp"
#include "cucumber/messages/Envelope.hpp"
#include "cucumber/messages/TestStepResultStatus.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <fmt/core.h>
#include <fmt/ostream.h>
#include <memory>
#include <ostream>
#include <utility>

namespace cucumber::pretty_formatter
{
    ProgressPrinter::ProgressPrinter(std::ostream& stream, std::shared_ptr<Theme> theme, std::size_t maxWidth)
        : stream{ stream }
        , theme{ std::move(theme) }
        , maxWidth{ maxWidth }
        , width{ 0 }
    {}

    void ProgressPrinter::Update(const messages::Envelope& envelope)
    {
        if (envelope.testRunHookFinished.has_value())
        {
            PrintStatus(envelope.testRunHookFinished.value()->result->status);
        }
        if (envelope.testStepFinished.has_value())
        {
            PrintStatus(envelope.testStepFinished.value()->testStepResult->status);
        }
        if (envelope.testRunFinished.has_value())
        {
            stream << "\n";
        }
    }

    void ProgressPrinter::PrintStatus(messages::TestStepResultStatus status)
    {
        const auto icon = theme->ProgressIcon(status);
        const auto newLine = (++width % maxWidth) == 0;
        if (newLine)
        {
            width = 0;
            fmt::print(stream, "{}\n", theme->Style(Theme::Element::progressIcon, status, icon));
        }
        else
        {
            fmt::print(stream, "{}", theme->Style(Theme::Element::progressIcon, status, icon));
        }
        stream.flush();
    }
}
