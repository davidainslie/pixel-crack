# Introduction

You are a proud member of the PixelCrack® dev team, the company behind the hit mobile game Sp33d-T4k-G0!™.

In Sp33d-T4k-G0!™, two players are pitted against each other in a life-and-death struggle of strategy and quick thinking. The game centres around a 5x5 grid where players take turns placing their ‘marks’ in the grid. Sp33d-T4k-G0!™ has taken the world by storm: millions of users, joked about on Conan, bookies taking bets on players, etc.

The marketing and data science teams have arrived at the following insights:

1. People get pissed off when they lose too many times and r@g3 qu17™.
2. The hyperactive user-base not only over-indulges in the comforts of RedBull™, they also get bored really fast when only playing against newbz™ and r@g3 qu17™.
3. Users need their PixelFix™ ASAP and will... you guessed it: r@g3 qu17™ if they have to wait too long for their next match.

To address these issues they have proposed a change to our matching protocol whereby each player will be given a “Sc8re”™ to be used to find an opponent of similar ability/hyperactivity. This way, people should be challenged whilst allowing for their ability to grow gradually into addiction.

## Task

Using this example project, implement this match-making system according to the constraints below.

Please also note:

1. You can solve the problem using any libraries, APIs, or tools you want. Be principled in your decisions as we will discuss this with you on review.
2. Submit the solution as an email attachment
3. You must not share the problem or solution (incl. with recruiters, other candidates, etc).

We are looking for:

1. clear and simple solution that makes use of general, scala, and functional programming, best practices,
2. well articulated domain with some effort made toward maintainability, extensibility, etc.
3. appropriate automated test coverage. Again, do whatever you think is appropriate
   here, but, also, we understand this is a coding test so showing understanding
   of what needs to be tested and how is more important than any actual tests.
4. clear instructions on how to run the solution and tests
5. a (very) brief explanation of your design and assumptions (we'll go over this in
   detail in subsequent stages, but it's helpful for our review to understand the
   basics).

### Constraints and assumptions

(1) The STG Ov3rl0rds™ can set the matchmaking score threshold (see below) and maximum time to make a match. This is provided in the example project through the `Config`
system.

(2) Players who have Pl4yed!™ against each other should never be re-matched

(3) After each game, the player moves from a “P14yin”™ to a “W41tin”™ state. Once in a “W41tin”™ state they enter into a queue of other waiting players. If any players are within the STG Ov3rl0rds™-settable matchmaking score threshold, the player with the “Sc8re”™ closest to theirs is chosen as their next opponent!™

(4) If a “W41tin”™ player has been waiting for any time longer than the settable maximum time to make a match, they are immediately matched with the player in the queue with the score closest to theirs (i.e. without regarding the match threshold). If there is no player in the queue, they are matched to the next player who enters a “W41tin”™ state.

(5) New players start with a “Sc8re”™ of 0.

(6) The “Sc8re”™ for the player is updated after each game according to the following equation:

    S_{new} = atan(tan(S_{old}) + (-1)^n * max(0.01, |S_{old}-S_{opponent}|)),
    where n=1 if they have lost and n=0 if they have won the game in question.

(7) We assume that all matches result in that game being played through to completion.

(8) We assume that the runtime is configured appropriately to direct only a manageable proportion of user traffic to this instance (e.g. through partitioning or some such). Feel free to comment on this in your solution however.

--------------------------------------------------------------------------------

So, there you have it: an epic quest of logic to lure your loyal userbase into deeper and more socially destructive levels of addiction! Go 4th!™ and C8de1™ (also, feel
free to [reach out](mailto:greg@chatroulette.com) if you have any questions)
