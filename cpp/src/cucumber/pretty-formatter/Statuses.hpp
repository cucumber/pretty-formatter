#ifndef CUCUMBER_PRETTY_FORMATTER_STATUSES_HPP
#define CUCUMBER_PRETTY_FORMATTER_STATUSES_HPP

#include "cucumber/messages/TestStepResultStatus.hpp"
#include <algorithm>
#include <iterator>
#include <set>

namespace cucumber::pretty_formatter
{
    inline const std::set allStatuses = {
        messages::TestStepResultStatus::UNKNOWN,
        messages::TestStepResultStatus::PASSED,
        messages::TestStepResultStatus::SKIPPED,
        messages::TestStepResultStatus::PENDING,
        messages::TestStepResultStatus::UNDEFINED,
        messages::TestStepResultStatus::AMBIGUOUS,
        messages::TestStepResultStatus::FAILED,
    };

    inline const std::set nonFailedStatuses = {
        messages::TestStepResultStatus::PASSED,
        messages::TestStepResultStatus::SKIPPED,
    };

    inline const std::set failingStatuses = []()
    {
        std::set<messages::TestStepResultStatus> result;
        std::set_difference(allStatuses.begin(), allStatuses.end(), nonFailedStatuses.begin(), nonFailedStatuses.end(),
            std::inserter(result, result.end()));
        return result;
    }();
}

#endif
