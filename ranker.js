/*

The source file is assumed to be composed solely of lines with the following structure:
player1name player2name player1score player2score

Every field is separated by a tab. There should be no empty lines in the source file.

Also, the source file is assumed to be at the same folder than the script (though it's trivial to change this, just go to the last line of code and change it).

To call the program, enter "node ranker.js source.txt" at the command line.

*/

var node = {
   fs: require ('fs'),
}

function calculateRanking (path) {

   var initialScore = 1000;

   var record = [];
   var sourceFile = node.fs.readFileSync (path, 'utf8');
   sourceFile = sourceFile.split ('\n');
   sourceFile.pop ();
   for (var line in sourceFile) {
      var elements = sourceFile [line].split ('\t');
      if (elements === undefined || elements.length !== 4 || (elements [2] - elements [3] === 0) || (elements [2] < 21 && elements [3] < 21)) {
         return ('Invalid source! Error is at line ' + (parseInt (line) + 1) + ': "' + sourceFile [line] + '"');
      }
      var player1 = elements [0].toLowerCase ();
      var player2 = elements [1].toLowerCase ();
      var score1 = parseInt (elements [2]);
      var score2 = parseInt (elements [3]);
      var match = {};
      match [player1] = score1;
      match [player2] = score2;
      record.push (match);
   }


   var scores = {};

   for (var match in record) {

      // This array intends to hold only two names.
      var opponents = [];

      for (var player in record [match]) {
         opponents.push (player);
         // We initialize the player if it doesn't exist in the scores object.
         if (scores [player] === undefined) {
            scores [player] = initialScore;
         }
      }
      // Ugly, but what are the alternatives? Doing everything recursively or calling a DB (which is a side effect anyway). I'll have some eval.
      var updatedScores = eval (scores);

      function expectedResult (i) {
         return (1 / (1 + Math.pow (10, (i.playerScore - i.opponentScore) / 400)));
      }

      function weightForMatch (i) {
         var w = i / 10;
         return (w > 10) ? w : 10;
      }

      function scoreDifference (i) {
         return Math.abs(i.playerScore - i.opponentScore);
      }

      function rankingDelta (i) {
         var w = weightForMatch (i.scoreDifference);
         return (w * i.actualResult - i.expectedResult);
      }

      function isWinner (i) {
         for (var player in i.match) {
            if (player !== i.player) {
               if (i.match [player] > i.match [i.player]) {
                  return 0;
               }
               else {
                  return 1;
               }
            }
         }
      }

      scores [opponents [0]] = updatedScores [opponents [0]] + rankingDelta ({
         actualResult: isWinner ({
            match: record [match],
            player: opponents [0],
         }),
         expectedResult: expectedResult ({
            playerScore: updatedScores [opponents [0]],
            opponentScore: updatedScores [opponents [1]],
         }),
         scoreDifference: scoreDifference ({
            playerScore: updatedScores [opponents [1]],
            opponentScore: updatedScores [opponents [0]],
         })
      });

      scores [opponents [1]] = updatedScores [opponents [1]] + rankingDelta ({
         actualResult: isWinner ({
            match: record [match],
            player: opponents [1],
         }),
         expectedResult: expectedResult ({
            playerScore: updatedScores [opponents [1]],
            opponentScore: updatedScores [opponents [0]],
         }),
         scoreDifference: scoreDifference ({
            playerScore: updatedScores [opponents [1]],
            opponentScore: updatedScores [opponents [0]],
         })
      });
   }

   for (var player in scores) {
      scores [player] = parseInt (scores [player]);
   }

   var sortedScores = [];
   for (var player in scores) {
      sortedScores.push ([player, scores [player]]);
   }
   sortedScores.sort (function (a, b) {return b [1] - a [1]});

   var ranking = {};
   for (var player in sortedScores) {
      ranking [sortedScores [player] [0]] = sortedScores [player] [1];
   }

   return ranking;
}

console.log (calculateRanking (__dirname + '/' + process.argv [2]));
