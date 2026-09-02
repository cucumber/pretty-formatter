#include "cucumber/pretty-formatter/ProgressBarPrinter.hpp"
#include "cucumber/messages/Envelope.hpp"
#include "cucumber/messages/TestCase.hpp"
#include "cucumber/messages/TestCaseFinished.hpp"
#include "cucumber/messages/TestCaseStarted.hpp"
#include "cucumber/messages/TestRunFinished.hpp"
#include "cucumber/messages/TestRunHookFinished.hpp"
#include "cucumber/messages/TestRunStarted.hpp"
#include "cucumber/messages/TestStepFinished.hpp"
#include "cucumber/messages/UndefinedParameterType.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include "cucumber/query/Query.hpp"
#include <cstddef>
#include <fmt/core.h>
#include <fmt/ostream.h>
#include <memory>
#include <ostream>
#include <string>
#include <string_view>
#include <utility>

namespace cucumber::pretty_formatter
{
    struct ProgressBarPrinter::Data : query::Query
    {};

    struct ProgressBarPrinter::Printer
    {
        Printer(std::ostream& stream, Data& data, std::string_view clearLine)
            : stream{ stream }
            , data{ data }
            , clearLine{ clearLine }
        {}

        ~Printer() = default;

        Printer(const Printer&) = delete;
        Printer& operator=(const Printer&) = delete;

        Printer(Printer&&) = delete;
        Printer& operator=(Printer&&) = delete;

        void UndefinedParameterType(const std::shared_ptr<const messages::UndefinedParameterType>& undefinedParameterType)
        {}

        void TestRunStarted(const std::shared_ptr<const messages::TestRunStarted>& testRunStarted)
        {
            ReRender(true);
        }

        void TestCase(const std::shared_ptr<const messages::TestCase>& testCase)
        {}

        void TestRunHookFinished(const std::shared_ptr<const messages::TestRunHookFinished>& testRunHookFinished)
        {}

        void TestCaseStarted(const std::shared_ptr<const messages::TestCaseStarted>& testCaseStarted)
        {}

        void TestStepFinished(const std::shared_ptr<const messages::TestStepFinished>& testStepFinished)
        {}

        void TestCaseFinished(const std::shared_ptr<const messages::TestCaseFinished>& testCaseFinished)
        {}

        void TestRunFinished(const std::shared_ptr<const messages::TestRunFinished>& testRunFinished)
        {}

    private:
        void ReRender(bool initial)
        {
            std::string output;

            output = fmt::format("[testRunStarted]\n");
            output += fmt::format("  \n");
            output += fmt::format("  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ 0 / 0 scenarios\n");
            output += fmt::format("  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ 0 / 0 steps\n");
            output += fmt::format("  Getting ready...\n");
            output += fmt::format("  \n");

            Render(initial, output);

            output = fmt::format("[testCase]\n");
            output += fmt::format("  \n");
            output += fmt::format("  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ 0 / 1 scenarios\n");
            output += fmt::format("  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ 0 / 3 steps\n");
            output += fmt::format("  Getting ready...\n");
            output += fmt::format("  ");

            Render(false, output);
        }

        void Render(bool initial, std::string_view output)
        {
            if (!initial)
            {
                fmt::print(stream, fmt::runtime(clearLine), 4);
            }
            fmt::print(stream, "{}", output);
            stream.flush();
        }

        std::ostream& stream;
        Data& data;
        std::string_view clearLine;
    };

    ProgressBarPrinter::ProgressBarPrinter(std::ostream& stream, std::unique_ptr<Theme> theme, std::size_t maxWidth,
        std::string_view clearLine)
        : theme{ std::move(theme) }
        , data{ std::make_unique<Data>() }
        , printer{ std::make_unique<Printer>(stream, *data, clearLine) }
    {}

    ProgressBarPrinter ::~ProgressBarPrinter() = default;

    void ProgressBarPrinter::Update(const messages::Envelope& envelope)
    {
        data->Update(envelope);

        if (envelope.undefinedParameterType.has_value())
        {
            printer->UndefinedParameterType(envelope.undefinedParameterType.value());
        }
        if (envelope.testRunStarted.has_value())
        {
            printer->TestRunStarted(envelope.testRunStarted.value());
        }
        if (envelope.testCase.has_value())
        {
            printer->TestCase(envelope.testCase.value());
        }
        if (envelope.testRunHookFinished.has_value())
        {
            printer->TestRunHookFinished(envelope.testRunHookFinished.value());
        }
        if (envelope.testCaseStarted.has_value())
        {
            printer->TestCaseStarted(envelope.testCaseStarted.value());
        }
        if (envelope.testStepFinished.has_value())
        {
            printer->TestStepFinished(envelope.testStepFinished.value());
        }
        if (envelope.testCaseFinished.has_value())
        {
            printer->TestCaseFinished(envelope.testCaseFinished.value());
        }
        if (envelope.testRunFinished.has_value())
        {
            printer->TestRunFinished(envelope.testRunFinished.value());
        }
    }
}
