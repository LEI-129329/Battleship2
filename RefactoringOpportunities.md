| Local | Cheiro no código | Refabricação | N.º de aluno |
|---|---|---|---|
| `Ship::Ship` | atribuições redundantes | simplificar construtor | 113044 |
| `Ship::getTopMostPos`, `getBottomMostPos`, `getLeftMostPos`, `getRightMostPos` | Código duplicado | Extract Method | 113044 |
| `Move::processEnemyFire` | Long Method — agregava e contabilizava os resultados dos tiros | Extract Method | 123572 |
| `Game::jsonShots` | Large Class —  Game concentra estado e regras da partida, sendo muito complexo e grande , sendo assim fazia sentido o método jsonShots ter uma classe própria, diminuindo a complexidade  | Extract Class | 123572 |
| `Game::randomEnemyFire` | Long Method — misturava seleção, impressão e processamento dos tiros | Extract Method  | 123572 |
| `Fleet::collisionRisk` | Nome com erro ortográfico | Rename | 101283 |
| `Fleet::printStatus` | Message Chains — expressão encadeada em `getFloatingShips().size()` | Introduce Variable | 101283 |
| `Fleet::createRandom` | Magic values — array de tipos de navio criado a cada chamada | Introduce Constant | 101283 |
| `Fleet::addShip` | Condição complexa no `if` | Extract Method | 101283 |
| `Fleet::printShips` | Feature Envy — não usa estado da instância | Make Static | 101283 |
| `Fleet::printAllShips` | Visibilidade inconsistente com os outros métodos `print*` | Change Signature | 101283 |
| `Caravel::Caravel`, `Carrack::Carrack`, `Frigate::Frigate` | Duplicated Code — repetição da lógica de construção de posições para diferentes orientações | Pull Members Up | 129329 |
| `Tasks::menu` | Long Method / Switch Statements — método extenso que concentra a interpretação e execução de vários comandos | Extract Method | 129329 |
| `Galleon::Galleon`, `fillNorth`, `fillSouth`, `fillEast`, `fillWest` | Switch Statements — lógica condicional de construção das posições dependente da orientação | Decompose Conditional | 129329 |
| `Game::printShips`, `Game::printBoardShots`, `BattleshipGraphicalView` — métodos correspondentes | Duplicated Code — lógica comum de mapeamento das células do tabuleiro | Extract Class | 129329 |