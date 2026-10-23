.class public final Lcom/dbg/Coords;
.super Ljava/lang/Object;

.method public static apply(Lcom/tvsm/connect/onboarding/ui/onBoarding/OnBoardingActivityNew;Ljava/lang/String;)Z
    .locals 5

    const-string v0, "Coords.apply called"

    invoke-static {v0}, Lcom/dbg/L;->d(Ljava/lang/String;)V

    const-string v0, "(-?\\d{1,3}\\.\\d{3,})\\s*,\\s*(-?\\d{1,3}\\.\\d{3,})"

    invoke-static {v0}, Ljava/util/regex/Pattern;->compile(Ljava/lang/String;)Ljava/util/regex/Pattern;

    move-result-object v0

    invoke-virtual {v0, p1}, Ljava/util/regex/Pattern;->matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;

    move-result-object v0

    invoke-virtual {v0}, Ljava/util/regex/Matcher;->find()Z

    move-result v1

    if-nez v1, :found

    const/4 v0, 0x0

    return v0

    :found
    const/4 v1, 0x1

    invoke-virtual {v0, v1}, Ljava/util/regex/Matcher;->group(I)Ljava/lang/String;

    move-result-object v2

    const/4 v1, 0x2

    invoke-virtual {v0, v1}, Ljava/util/regex/Matcher;->group(I)Ljava/lang/String;

    move-result-object v3

    const-string v4, ""

    const-string v0, "Shared location"

    invoke-virtual {p0, v2, v3, v4, v0}, Lcom/tvsm/connect/onboarding/ui/onBoarding/OnBoardingActivityNew;->z(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    const/4 v0, 0x1

    return v0
.end method
