这是一个开源项目，当前polymarket bot专注于监控天气相关的套利，套利方式是通过官方的https://github.com/Polymarket/py-builder-relayer-client 来调用**NegRiskAdapter**合约上的convertPositions接口来执行。

convertPositions 套利的原理如下：

假设一个event有 N 个问题，用户提交了 k 个 NO token（每种 _amount个）。转换后用户获得：

| 产出 | 数量 |
| --- | --- |
| 抵押品（collateral，usdc） | (k - 1) * amountOut |
| YES token（互补问题的） | 每个 (N - k) 个问题各 amountOut |

如果转换前后有差价，则就能套利，当然转换前，需要立刻买入no token

为了实现天气套利目标，工程上需要实现如下内容

1. 通过list event 接口获取数据，筛选出来符合要求的tokenId，这块逻辑是否需要入库？入库可能会方便些，例如有些market closed了，后续就应该取消订阅。
2. 通过wss实时监控tokenId的报价和流动性深度，同时获取这个event 下面各个market里的tokenId报价
3. 如果捕捉到了套利机会，记录到数据库，记录的内容是买入哪些no tokenId，假设可以买入成功，并假设做了convertPositions 操作，之后可以获得的usdc和yes token。
4. 解析链上positionsConvert 事件，并入库
5. 将我记录到的套利机会数据和链上实际发生的positionsConvert事件进行对比，以链上事件为标准，因为这是现在正在套利高手的操作。 对于同一个tokenId，同一个时间，我是否多操作了或者少操作或者我买入是否太多以至于交易没法完成。
6. 当走完上述过程，确认自己没啥问题的时候，用官方relayer-client 尝试交易