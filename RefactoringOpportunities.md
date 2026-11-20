| Local | Cheiro no código | Refabricação | N.º de aluno |
|---|---|---|---|
| `Ship::Ship` | atribuições redundantes | simplificar construtor | 113044 |
| `Ship::getTopMostPos`, `getBottomMostPos`, `getLeftMostPos`, `getRightMostPos` | Código duplicado | Extract Method | 113044 |
| `Ship::tooCloseTo(IShip)` | Uso explícito de `Iterator` | Substituir por for-each | 113044 |
| `Ship::isInsideBoard` | Asserção redundante | Remove Redundant Assertion | 113044 |
| `Ship::verificarNavio` | Feature Envy (`MethodMayBeStatic`) | Make Method Static | 113044 |