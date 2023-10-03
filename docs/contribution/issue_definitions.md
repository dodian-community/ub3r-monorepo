# Issue Definitions
This document will explain what's preferred to be included in a submitted issue. We request that you follow these guidelines to the best of your abilities.

---

<details>
<summary>Navigation Menu</summary>

<ul>
    <li><a href="/docs/contribution">Guides</a>
        <ul>
            <li><a href="/docs/guides/getting_started.md">Getting Started</a></li>
            <li><a href="/docs/guides/installing_mysql.md">Installing MySQL Database</a></li>
            <li style="margin-top: 5px"><a href="/docs/guides/glossary.md">Glossary</a></li>
        </ul>
    </li>
    <li><a href="/docs/contribution">Contribution</a>
        <ul>
            <li><a href="/docs/contribution/guidelines.md">Contribution Guidelines</a></li>
            <li><a href="/docs/contribution/issue_definitions.md">Issue Definitions</a></li>
        </ul>
    </li>
    <li><a href="/docs/development">Development</a>
        <ul>
            <li><a href="/docs/development/database.md">Ub3r Database</a></li>
        </ul>
    </li>
    <li><a href="/docs/other">Other</a>
        <ul>
            <li><a href="/docs/other/environment_variables.md">Environment Variables</a></li>
        </ul>
    </li>
</ul>

</details>

---

### Issue Title

The issue title should be as descriptive as one sentence lets you be. Should preferably also follow [title casing rules](https://apastyle.apa.org/style-grammar-guidelines/capitalization/title-case), _although that's more of a preference for the sake of consistency_.

### Issue Content

Some lists that define what we hope to see included in submitted issues. Not everything in these lists is necessary applicable for all issues, so add the things that are.

#### Bug Report

Submitted when something isn't working as intended. Either visually, functionally, or both.

- Problem Details
    - Affected Areas, Objects, Features, etc
        - What is affected by the bug - sets the context for the rest of the issue
    - Problem Type
        - Can for example be `visual`, `annoyance`, and/or `broken feature` at all
    - Happens When
        - Can for example be `when interacting with it`
    - In-com.jagex.runescape.Game Requirements
        - Can for example be `levels` and/or `items`
            - **Example:** Agility level between 10 and 40
            - **Example:** Not have red key in your inventory or bank
        - _Mostly just relevant for reproduction_
- Horizontal Separator Line
    - **_This is only applicable if your issue includes problem details_**
    - _Type `---` to achieve this separator_
- Problem Description
    - _A more in depth explanation of the issue's title_
    - _May also include media, such as GIFs, videos, and/or images_
- What's Expected to Happen
    - _May also include media, such as GIFs, videos, and/or images_
- What's Happening Instead
    - _May also include media, such as GIFs, videos, and/or images_
- Reproduction Recipe
    - _May also include a video showing how to reproduce_
- Possible Causes to Investigate _**(optional)**_
    - _If you have theories or definitive answers as to what in the code causes this, you may include this, **but you don't have to!**_

**_Bug Report Examples for Reference:_**
- [#6 - Trading Dupe, Allows Duplicating Items With Trading](https://github.com/dodian-community/ub3r-monorepo/issues/6)
- [#16 - Barbarian Village Agility Course, Rope Swing Not Working](https://github.com/dodian-community/ub3r-monorepo/issues/16)

#### Feature Request

Submitted when you have ideas for features that would improve the playing experience of Dodian. Can also be content, such as bosses, mini-games, etc.

- Feature Description
    - _A more in depth explanation of the issue's title_
- This Feature Would Solve
    - _Some context on how **you** see this feature benefit Dodian_
- Possible Solutions
    - _If you have theories, ideas, or definitive answers on how this can be implemented, you may include this, **but you don't have to!**_

**_Feature Request Examples for Reference:_**
- [#14 - Ability to use Complicated Passwords](https://github.com/dodian-community/ub3r-monorepo/issues/14)
- [#13 - Self Updating com.jagex.runescape.Game Client](https://github.com/dodian-community/ub3r-monorepo/issues/13)