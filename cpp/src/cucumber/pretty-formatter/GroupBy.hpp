#ifndef CUCUMBER_PRETTY_FORMATTER_GROUP_BY_HPP
#define CUCUMBER_PRETTY_FORMATTER_GROUP_BY_HPP

#include "cucumber/messages/TestStepResultStatus.hpp"
#include <functional>
#include <map>
#include <memory>
#include <vector>

namespace cucumber::pretty_formatter
{
    template<typename Proj, typename Instance, typename ListType>
    auto GroupBy(Instance&& instance, Proj&& proj, const std::vector<std::shared_ptr<const ListType>>& list)
    {
        std::map<messages::TestStepResultStatus, std::vector<std::shared_ptr<const ListType>>> groupedByProj;

        for (const auto& item : list)
        {
            groupedByProj[std::invoke(std::forward<Proj>(proj), std::forward<Instance>(instance), item)].push_back(item);
        }

        return groupedByProj;
    }

    template<typename Proj, typename ListType>
    auto GroupBy(Proj&& proj, const std::vector<std::shared_ptr<const ListType>>& list)
    {
        std::map<messages::TestStepResultStatus, std::vector<std::shared_ptr<const ListType>>> groupedByProj;

        for (const auto& item : list)
        {
            groupedByProj[std::invoke(std::forward<Proj>(proj), item)].push_back(item);
        }

        return groupedByProj;
    }
}

#endif
