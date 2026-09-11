#ifndef CUCUMBER_PRETTY_FORMATTER_GROUP_BY_HPP
#define CUCUMBER_PRETTY_FORMATTER_GROUP_BY_HPP

#include "cucumber/messages/TestStepResultStatus.hpp"
#include <functional>
#include <iterator>
#include <map>
#include <type_traits>
#include <vector>

namespace cucumber::pretty_formatter
{
    template<typename Proj, typename Instance, typename Container>
    auto GroupBy(Instance&& instance, Proj&& proj, const Container& list)
    {
        using ElementType = std::remove_reference_t<decltype(*std::begin(list))>;

        std::map<messages::TestStepResultStatus, std::vector<std::reference_wrapper<ElementType>>> groupedByProj;

        for (const auto& item : list)
        {
            groupedByProj[std::invoke(std::forward<Proj>(proj), std::forward<Instance>(instance), item)].emplace_back(item);
        }

        return groupedByProj;
    }

    template<typename Proj, typename Container>
    auto GroupBy(Proj&& proj, const Container& list)
    {
        using ElementType = std::remove_reference_t<decltype(*std::begin(list))>;

        std::map<messages::TestStepResultStatus, std::vector<std::reference_wrapper<ElementType>>> groupedByProj;

        for (const auto& item : list)
        {
            groupedByProj[std::invoke(std::forward<Proj>(proj), item)].emplace_back(item);
        }

        return groupedByProj;
    }
}

#endif
