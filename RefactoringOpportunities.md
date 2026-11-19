| Local | Cheiro no código | Refabricação | N.º de aluno |
|---|---|---|---|
| `Ship::Ship` | atribuições redundantes | simplificar construtor | 113044 |
| `Ship::getTopMostPos`, `getBottomMostPos`, `getLeftMostPos`, `getRightMostPos` | Código duplicado | Extract Method | 113044 |
| `Move::processEnemyFire` | Long Method — agregava e contabilizava os resultados dos tiros | Extract Method | 123572 |
| `Game::jsonShots` | Large Class —  Game concentra estado e regras da partida, sendo muito complexo e grande , sendo assim fazia sentido o método jsonShots ter uma classe própria, diminuindo a complexidade  | Extract Class | 123572 |
| `Game::randomEnemyFire` | Long Method — misturava seleção, impressão e processamento dos tiros | Extract Method  | 123572 |
